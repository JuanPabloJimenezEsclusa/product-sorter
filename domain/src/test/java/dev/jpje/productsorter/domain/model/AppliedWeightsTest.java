package dev.jpje.productsorter.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Map;
import java.util.stream.Stream;

import dev.jpje.productsorter.domain.vo.SalesUnits;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AppliedWeightsTest {

  @ParameterizedTest(name = "{0}")
  @MethodSource("validWeightsScenarios")
  void shouldCreateWithValidWeights(final double sales, final double stock) {
    final var weights = new AppliedWeights(sales, stock);
    assertThat(weights.salesUnitsWeight()).as("sales weight").isEqualTo(sales);
    assertThat(weights.stockWeight()).as("stock weight").isEqualTo(stock);
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
    assertThat(weights.salesUnitsWeight()).as("sales weight from map").isEqualTo(expectedSales);
    assertThat(weights.stockWeight()).as("stock weight from map").isEqualTo(expectedStock);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("fromMapErrorScenarios")
  void shouldPropagateValidationErrorsFromMap(final Map<String, Double> map) {
    assertThatThrownBy(() -> AppliedWeights.fromMap(map))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("computeScoreScenarios")
  void shouldComputeScore(final AppliedWeights weights, final SalesUnits sales,
                           final String stockRaw, final double midpoint, final double expectedScore) {
    final var stock = StockMother.from(stockRaw);
    final var score = weights.computeScore(sales, stock, midpoint);
    assertThat(score).as("computed score for %s", stockRaw).isCloseTo(expectedScore, within(0.001));
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

  private static Stream<Arguments> fromMapErrorScenarios() {
    return Stream.of(
      arguments(named("null map", (Map<String, Double>) null)),
      arguments(named("empty map", Map.<String, Double>of())),
      arguments(named("unknown key", Map.of("unknown", 0.5))),
      arguments(named("out of range weight", Map.of("salesUnits", -0.1))));
  }

  private static Stream<Arguments> computeScoreScenarios() {
    final var sales100 = SalesUnits.of(100);
    final var sales0 = SalesUnits.of(0);

    return Stream.of(
      arguments(named("both weights", new AppliedWeights(0.7, 0.3)), sales100, "S:1,M:1,L:0", 50.0,
        0.7 * (100.0 / 150.0) + 0.3 * (2.0 / 3.0)),
      arguments(named("sales only", new AppliedWeights(1.0, 0.0)), sales100, "S:1,M:1,L:1", 50.0,
        100.0 / 150.0),
      arguments(named("stock only", new AppliedWeights(0.0, 1.0)), sales100, "S:1,M:1,L:1", 50.0,
        1.0),
      arguments(named("both zero", new AppliedWeights(0.0, 0.0)), sales100, "S:1,M:1,L:1", 50.0,
        0.0),
      arguments(named("zero sales", new AppliedWeights(0.7, 0.3)), sales0, "S:1,M:1,L:0", 50.0,
        0.3 * (2.0 / 3.0)),
      arguments(named("different midpoint", new AppliedWeights(0.7, 0.3)), sales100, "S:1,M:1,L:0", 100.0,
        0.7 * 0.5 + 0.3 * (2.0 / 3.0)));
  }
}
