package com.acidtango.productsorter.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class MdcFilterTest {

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

  @ParameterizedTest(name = "{0}")
  @MethodSource("cleanupCases")
  void shouldCleanMdcAfterRequest(final MockHttpServletRequest request) throws Exception {
    MDC.put("existing", "value");
    filter.doFilterInternal(request, new MockHttpServletResponse(), (_, _) -> {});
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
      arguments(named("with header", new MockHttpServletRequest("GET", "/api/products") {{
        addHeader("X-Request-Id", "abc");
      }})));
  }
}
