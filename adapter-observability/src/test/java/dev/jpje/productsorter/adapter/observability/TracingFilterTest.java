package dev.jpje.productsorter.adapter.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.ServiceAttributes;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TracingFilterTest {

  private final RecordingSpanExporter exporter = new RecordingSpanExporter();
  private final TracingFilter filter = new TracingFilter("test", openTelemetry(exporter));

  @Test
  void shouldCreateExactlyOneServerSpanPerRequest() throws Exception {
    filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/products"),
      new MockHttpServletResponse(), (_, _) -> {
      });

    assertThat(exporter.spans())
      .as("TracingFilter must be the single SERVER span source for one request")
      .hasSize(1);
    assertThat(exporter.spans().getFirst().getKind())
      .as("the manual request span must be a SERVER span")
      .isEqualTo(SpanKind.SERVER);
  }

  @Test
  void shouldFlushBufferedSpansOnShutdown() throws Exception {
    final var method = TracingConfig.class.getMethod("openTelemetry", String.class, String.class);

    assertThat(AnnotationUtils.findAnnotation(method, Bean.class).destroyMethod())
      .as("graceful shutdown must flush batched spans")
      .isEqualTo("close");
  }

  @Test
  void shouldConfigureW3cTracePropagation() {
    final var openTelemetry = new TracingConfig().openTelemetry("test", "http://localhost:4318/v1/traces");
    try {
      final var inboundTraceId = "0af7651916cd43dd8448eb211c80319c";
      final var request = new MockHttpServletRequest();
      request.addHeader("traceparent", "00-" + inboundTraceId + "-b7ad6b7169203331-01");

      final var extracted = openTelemetry.getPropagators().getTextMapPropagator()
        .extract(Context.root(), request, TracingFilter.HttpRequestHeaderGetter.INSTANCE);

      assertThat(Span.fromContext(extracted).getSpanContext().getTraceId())
        .as("TracingConfig must configure W3C propagation, else inbound trace context is never extracted")
        .isEqualTo(inboundTraceId);
    } finally {
      ((OpenTelemetrySdk) openTelemetry).close();
    }
  }

  @Test
  void shouldContinueInboundTraceContext() throws Exception {
    final var continuing = new RecordingSpanExporter();
    final var continuingFilter = new TracingFilter("test", openTelemetry(continuing));
    final var inboundTraceId = "0af7651916cd43dd8448eb211c80319c";
    final var request = new MockHttpServletRequest("GET", "/api/v1/products");
    request.addHeader("traceparent", "00-" + inboundTraceId + "-b7ad6b7169203331-01");

    continuingFilter.doFilter(request, new MockHttpServletResponse(), (_, _) -> {
    });

    assertThat(continuing.spans()).hasSize(1);
    assertThat(continuing.spans().getFirst().getTraceId())
      .as("an inbound traceparent must be continued, not replaced")
      .isEqualTo(inboundTraceId);
  }

  @Test
  void shouldStartANewTraceWhenNoInboundContext() throws Exception {
    filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/products"),
      new MockHttpServletResponse(), (_, _) -> {
      });

    assertThat(exporter.spans().getFirst().getTraceId())
      .as("a request without inbound context must start a fresh, valid trace")
      .isNotEqualTo("00000000000000000000000000000000");
  }

  /**
   * Builds an SDK whose span processor records into the supplied exporter and whose
   * propagator is the W3C text-map propagator, mirroring {@code TracingConfig}.
   */
  static OpenTelemetry openTelemetry(final SpanExporter exporter) {
    final var provider = SdkTracerProvider.builder()
      .setResource(Resource.create(Attributes.of(ServiceAttributes.SERVICE_NAME, "test")))
      .addSpanProcessor(SimpleSpanProcessor.create(exporter))
      .build();
    return OpenTelemetrySdk.builder()
      .setTracerProvider(provider)
      .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
      .build();
  }

  static final class RecordingSpanExporter implements SpanExporter {

    private final List<SpanData> exported = new ArrayList<>();

    @Override
    public CompletableResultCode export(final Collection<SpanData> spans) {
      exported.addAll(spans);
      return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
      return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
      return CompletableResultCode.ofSuccess();
    }

    List<SpanData> spans() {
      return exported;
    }
  }
}
