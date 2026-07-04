package dev.jpje.productsorter.adapter.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.ServiceAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "otel.enabled", havingValue = "true", matchIfMissing = true)
public class TracingConfig {

  @Bean
  public OpenTelemetry openTelemetry(
    @Value("${otel.service-name:product-sorter}") final String serviceName,
    @Value("${otel.endpoint:http://localhost:4318/v1/traces}") final String endpoint) {
    final var resource = Resource.create(Attributes.of(ServiceAttributes.SERVICE_NAME, serviceName));
    final var spanExporter = OtlpHttpSpanExporter.builder().setEndpoint(endpoint).build();
    final var tracerProvider = SdkTracerProvider.builder()
      .setResource(resource)
      .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
      .build();
    return OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();
  }
}
