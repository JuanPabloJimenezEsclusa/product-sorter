package dev.jpje.productsorter.adapter.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
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
