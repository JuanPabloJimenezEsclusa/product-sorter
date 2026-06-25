package dev.jpje.productsorter.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AppliedWeightsTest {

  @ParameterizedTest(name = "{0}")
  @MethodSource("validWeightsScenarios")
  void shouldCreateWithValidWeights(final double sales, final double stock) {
    final var weights = new AppliedWeights(sales, stock);
    assertThat(weights.salesUnitsWeight()).isEqualTo(sales);
    assertThat(weights.stockWeight()).isEqualTo(stock);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidWeightsScenarios")
  void shouldRejectNegativeWeights(final double sales, final double stock) {
    assertThatThrownBy(() -> new AppliedWeights(sales, stock))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("fromMapScenarios")
  void shouldCreateFromMap(final Map<String, Double> map, final double expectedSales, final double expectedStock) {
    final var weights = AppliedWeights.fromMap(map);
    assertThat(weights.salesUnitsWeight()).isEqualTo(expectedSales);
    assertThat(weights.stockWeight()).isEqualTo(expectedStock);
  }

  private static Stream<Arguments> validWeightsScenarios() {
    return Stream.of(
      arguments(named("both", 0.7), 0.3),
      arguments(named("zero", 0.0), 0.0));
  }

  private static Stream<Arguments> invalidWeightsScenarios() {
    return Stream.of(
      arguments(named("negative sales", -0.1), 0.0),
      arguments(named("negative stock", 0.0), -0.1));
  }

  private static Stream<Arguments> fromMapScenarios() {
    return Stream.of(
      arguments(named("both weights", Map.of("salesUnits", 0.7, "stockRatio", 0.3)), 0.7, 0.3),
      arguments(named("sales only", Map.of("salesUnits", 1.0)), 1.0, 0.0));
  }
}
