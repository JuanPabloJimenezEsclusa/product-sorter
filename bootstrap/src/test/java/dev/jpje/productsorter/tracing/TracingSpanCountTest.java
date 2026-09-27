package dev.jpje.productsorter.tracing;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
import org.awaitility.core.ConditionTimeoutException;
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

  private static final Duration SPAN_ARRIVAL_TIMEOUT = Duration.ofSeconds(5);

  private static final Duration SPAN_POLL_INTERVAL = Duration.ofMillis(10);

  private static final Duration LATE_DUPLICATE_SETTLE = Duration.ofMillis(500);

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

    try {
      await()
        .atMost(SPAN_ARRIVAL_TIMEOUT)
        .pollInterval(SPAN_POLL_INTERVAL)
        .until(() -> !exportedServerSpans().isEmpty());
    } catch (ConditionTimeoutException _) {
      // Deliberately empty: the assertion below owns the failure.
    }

    TRACER_PROVIDER.forceFlush().join(SPAN_ARRIVAL_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
    awaitStableServerSpanCount();

    final var serverSpans = exportedServerSpans();
    assertThat(serverSpans)
      .as("one request must produce exactly one SERVER span, from the manual TracingFilter")
      .hasSize(1);
  }

  private List<SpanData> exportedServerSpans() {
    return EXPORTER.spans().stream()
      .filter(span -> span.getKind() == SpanKind.SERVER)
      .toList();
  }

  private void awaitStableServerSpanCount() {
    final var lastSize = new AtomicInteger(-1);
    final var lastChange = new AtomicLong(System.nanoTime());

    await()
      .atMost(SPAN_ARRIVAL_TIMEOUT)
      .pollInterval(SPAN_POLL_INTERVAL)
      .until(() -> {
        final int currentSize = exportedServerSpans().size();
        if (currentSize != lastSize.getAndSet(currentSize)) {
          lastChange.set(System.nanoTime());
          return false;
        }
        return System.nanoTime() - lastChange.get() >= LATE_DUPLICATE_SETTLE.toNanos();
      });
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
