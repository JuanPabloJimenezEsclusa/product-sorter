package dev.jpje.productsorter.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MetricsTest {

  @Test
  void shouldReturnKeyForSalesUnits() {
    assertThat(Metrics.SALES_UNITS.key()).isEqualTo("salesUnits");
  }

  @Test
  void shouldReturnKeyForStock() {
    assertThat(Metrics.STOCK.key()).isEqualTo("stockRatio");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("fromKeyScenarios")
  void shouldResolveFromKey(final String key, final Metrics expected) {
    assertThat(Metrics.fromKey(key)).isEqualTo(expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidKeyScenarios")
  void shouldRejectUnknownKey(final String key) {
    assertThatThrownBy(() -> Metrics.fromKey(key))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Unknown metric");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("validWeightsScenarios")
  void shouldAcceptValidWeights(final Map<String, Double> weights) {
    Metrics.validateWeights(weights);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidWeightsScenarios")
  void shouldRejectInvalidWeights(final Map<String, Double> weights) {
    assertThatThrownBy(() -> Metrics.validateWeights(weights))
      .isInstanceOf(IllegalArgumentException.class);
  }

  private static Stream<Arguments> fromKeyScenarios() {
    return Stream.of(
      arguments(named("salesUnits", "salesUnits"), Metrics.SALES_UNITS),
      arguments(named("stockRatio", "stockRatio"), Metrics.STOCK));
  }

  private static Stream<Arguments> invalidKeyScenarios() {
    return Stream.of(
      arguments(named("unknown", "unknown")),
      arguments(named("empty", "")));
  }

  private static Stream<Arguments> validWeightsScenarios() {
    return Stream.of(
      arguments(named("both", Map.of("salesUnits", 0.7, "stockRatio", 0.3))),
      arguments(named("sales only", Map.of("salesUnits", 1.0))),
      arguments(named("stock only", Map.of("stockRatio", 0.5))));
  }

  private static Stream<Arguments> invalidWeightsScenarios() {
    final var mapWithNull = new HashMap<String, Double>();
    mapWithNull.put("salesUnits", null);
    return Stream.of(
      arguments(named("null", (Object) null)),
      arguments(named("empty", Map.of())),
      arguments(named("unknown key", Map.of("unknown", 0.5))),
      arguments(named("negative", Map.of("salesUnits", -0.1))),
      arguments(named("over 1", Map.of("salesUnits", 1.5))),
      arguments(named("null value", mapWithNull)));
  }
}
