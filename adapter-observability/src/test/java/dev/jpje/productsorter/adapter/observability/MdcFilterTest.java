package dev.jpje.productsorter.adapter.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.stream.Stream;

import io.opentelemetry.api.trace.Span;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class MdcFilterTest {

  private static final String INBOUND_TRACE_ID = "0af7651916cd43dd8448eb211c80319c";

  private final MdcFilter filter = new MdcFilter();

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("requestCases")
  void shouldSetMdcContextInChain(final MockHttpServletRequest request,
                                  final boolean expectTraceId, final boolean expectRequestId) throws Exception {
    filter.doFilterInternal(request, new MockHttpServletResponse(), (_, _) -> {
      if (expectTraceId) {
        assertThat(MDC.get("traceId"))
          .as("traceId should be set inside chain")
          .matches(v -> v != null && !v.isBlank());
      }
      assertThat(MDC.get("requestUri"))
        .as("requestUri should match inside chain")
        .isEqualTo(request.getRequestURI());
      if (expectRequestId) {
        assertThat(MDC.get("requestId"))
          .as("requestId should be set inside chain")
          .isEqualTo("req-123");
      }
    });
  }

  @Test
  void shouldPreferActiveSpanTraceIdOverFallback() throws Exception {
    final var openTelemetry = TracingFilterTest.openTelemetry(new TracingFilterTest.RecordingSpanExporter());
    final var span = openTelemetry.getTracer("test").spanBuilder("operation").startSpan();

    try (final var _ = span.makeCurrent()) {
      filter.doFilterInternal(new MockHttpServletRequest("GET", "/api/v1/products"),
        new MockHttpServletResponse(), (_, _) ->
          assertThat(MDC.get("traceId"))
            .as("MDC traceId must equal the active span trace id")
            .isEqualTo(span.getSpanContext().getTraceId()));
    } finally {
      span.end();
    }
  }

  @Test
  void shouldCorrelateMdcWithTheActiveSpanAcrossBothFilters() throws Exception {
    assertThat(orderOf(TracingFilter.class))
      .as("TracingFilter must run before MdcFilter so a span is current when MDC is populated")
      .isLessThan(orderOf(MdcFilter.class));

    final var openTelemetry = TracingFilterTest.openTelemetry(new TracingFilterTest.RecordingSpanExporter());
    final var tracingFilter = new TracingFilter("test", openTelemetry);
    final var request = new MockHttpServletRequest("GET", "/api/v1/products");
    request.addHeader("traceparent", "00-" + INBOUND_TRACE_ID + "-b7ad6b7169203331-01");

    tracingFilter.doFilter(request, new MockHttpServletResponse(), (servletRequest, servletResponse) ->
      filter.doFilterInternal((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse,
        (_, _) -> {
          assertThat(Span.current().getSpanContext().getTraceId()).isEqualTo(INBOUND_TRACE_ID);
          assertThat(MDC.get("traceId"))
            .as("MDC traceId must equal the active span trace id")
            .isEqualTo(Span.current().getSpanContext().getTraceId());
        }));
  }

  @Test
  void shouldFallBackToShortRandomTraceIdWithoutSpan() throws Exception {
    filter.doFilterInternal(new MockHttpServletRequest("GET", "/api/v1/products"),
      new MockHttpServletResponse(), (_, _) ->
        assertThat(MDC.get("traceId")).as("fallback traceId is a short random id").hasSize(6));
  }

  private static int orderOf(final Class<?> filterType) {
    final var order = AnnotationUtils.findAnnotation(filterType, Order.class);
    return order != null ? order.value() : Ordered.LOWEST_PRECEDENCE;
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("cleanupCases")
  void shouldCleanMdcAfterRequest(final MockHttpServletRequest request) throws Exception {
    MDC.put("existing", "value");
    filter.doFilterInternal(request, new MockHttpServletResponse(), (_, _) -> {
    });
    assertThat(MDC.getCopyOfContextMap())
      .as("MDC should be cleared after request")
      .isNullOrEmpty();
  }

  private static Stream<Arguments> requestCases() {
    final var withTrace = new MockHttpServletRequest("GET", "/api/products");
    withTrace.addHeader("X-Request-Id", "req-123");

    final var withoutTrace = new MockHttpServletRequest("POST", "/api/sort");
    return Stream.of(
      arguments(named("with request id", withTrace), true, true),
      arguments(named("no request id", withoutTrace), true, false));
  }

  private static Stream<Arguments> cleanupCases() {
    return Stream.of(
      arguments(named("normal request", new MockHttpServletRequest("GET", "/test"))),
      arguments(named("with header", new MockHttpServletRequest("GET", "/api/products") {
        {
          addHeader("X-Request-Id", "abc");
        }
      })));
  }
}
