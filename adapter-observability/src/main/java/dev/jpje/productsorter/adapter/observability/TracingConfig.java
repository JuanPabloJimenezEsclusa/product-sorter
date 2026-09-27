package dev.jpje.productsorter.adapter.observability;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.ServiceAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "otel.enabled", havingValue = "true", matchIfMissing = true)
public class TracingConfig {

  /** {@code destroyMethod = "close"} shuts the SDK down, flushing spans buffered by the batch processor. */
  @Bean(destroyMethod = "close")
  public OpenTelemetrySdk openTelemetry(
    @Value("${otel.service-name:product-sorter}") final String serviceName,
    @Value("${otel.endpoint:http://localhost:4318/v1/traces}") final String endpoint) {
    final var resource = Resource.create(Attributes.of(ServiceAttributes.SERVICE_NAME, serviceName));
    final var spanExporter = OtlpHttpSpanExporter.builder().setEndpoint(endpoint).build();
    final var tracerProvider = SdkTracerProvider.builder()
      .setResource(resource)
      .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
      .build();
    return OpenTelemetrySdk.builder()
      .setTracerProvider(tracerProvider)
      .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
      .build();
  }
}
