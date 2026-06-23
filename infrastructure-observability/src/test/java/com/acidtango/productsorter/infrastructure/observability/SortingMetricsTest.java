package com.acidtango.productsorter.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SortingMetricsTest {

  private MeterRegistry registry;
  private SortingMetrics metrics;

  @BeforeEach
  void setUp() {
    registry = new SimpleMeterRegistry();
    metrics = new SortingMetrics(registry);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("recordCases")
  void shouldRecordSort(final int productCount, final Map<String, Double> weights) {
    metrics.recordSort(productCount, java.time.Duration.ofMillis(100), weights);

    assertThat(registry.counter("sorting.requests").count())
      .as("Request counter should increment")
      .isEqualTo(1);
    assertThat(registry.get("sorting.duration").timer().totalTime(TimeUnit.NANOSECONDS))
      .as("Duration should be recorded")
      .isPositive();
    assertThat(registry.get("sorting.products").summary().takeSnapshot().max())
      .as("Products count should match")
      .isEqualTo(productCount);

    if (weights.containsKey("salesUnits")) {
      assertThat(registry.get("sorting.weights").tag("criterion", "salesUnits").summary().takeSnapshot().max())
        .as("Sales weight should match")
        .isEqualTo(weights.get("salesUnits"));
    }
    if (weights.containsKey("stockRatio")) {
      assertThat(registry.get("sorting.weights").tag("criterion", "stockRatio").summary().takeSnapshot().max())
        .as("Stock weight should match")
        .isEqualTo(weights.get("stockRatio"));
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("multipleCallsCases")
  void shouldAccumulateMultipleCalls(final int times) {
    for (int i = 0; i < times; i++) {
      metrics.recordSort(10, java.time.Duration.ofMillis(50), Map.of("salesUnits", 0.5, "stockRatio", 0.5));
    }
    assertThat(registry.counter("sorting.requests").count())
      .as("Request count should accumulate")
      .isEqualTo(times);
    assertThat(registry.get("sorting.products").summary().takeSnapshot().count())
      .as("Products count should accumulate")
      .isEqualTo(times);
  }

  private static Stream<Arguments> recordCases() {
    return Stream.of(
      arguments(named("both weights", 100), Map.of("salesUnits", 0.7, "stockRatio", 0.3)),
      arguments(named("sales only", 50), Map.of("salesUnits", 1.0)),
      arguments(named("stock only", 200), Map.of("stockRatio", 0.5)),
      arguments(named("zero products", 0), Map.of()));
  }

  private static Stream<Arguments> multipleCallsCases() {
    return Stream.of(
      arguments(named("single call", 1)),
      arguments(named("multiple calls", 5)));
  }
}
