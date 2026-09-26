package dev.jpje.productsorter.tracing;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.ServiceAttributes;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:1/jwks",
    "spring.docker.compose.enabled=false",
    "otel.enabled=true"
  })
class TracingSpanCountTest {

  static final RecordingSpanExporter EXPORTER = new RecordingSpanExporter();

  static final SdkTracerProvider TRACER_PROVIDER = SdkTracerProvider.builder()
    .setResource(Resource.create(Attributes.of(ServiceAttributes.SERVICE_NAME, "recording")))
    .addSpanProcessor(SimpleSpanProcessor.create(EXPORTER))
    .build();

  @TestConfiguration
  static class RecordingTracingConfig {
    @Bean
    @Primary
    OpenTelemetry recordingOpenTelemetry() {
      return OpenTelemetrySdk.builder().setTracerProvider(TRACER_PROVIDER).build();
    }
  }

  @LocalServerPort
  private int port;

  @Test
  void shouldEmitExactlyOneServerSpanPerRequest() {
    EXPORTER.spans().clear();

    given().port(port).when().get("/actuator/prometheus").then().statusCode(200);
    TRACER_PROVIDER.forceFlush().join(5, TimeUnit.SECONDS);

    final var serverSpans = EXPORTER.spans().stream()
      .filter(span -> span.getKind() == SpanKind.SERVER)
      .toList();
    assertThat(serverSpans)
      .as("one request must produce exactly one SERVER span, from the manual TracingFilter")
      .hasSize(1);
  }

  static final class RecordingSpanExporter implements SpanExporter {

    private final List<SpanData> exported = new CopyOnWriteArrayList<>();

    @Override
    public CompletableResultCode export(final @NonNull Collection<SpanData> spans) {
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
