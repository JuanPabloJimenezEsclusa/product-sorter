package dev.jpje.productsorter.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.stream.Stream;

import dev.jpje.productsorter.domain.model.ScoreableProduct;
import dev.jpje.productsorter.domain.vo.ProductId;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ProductScorerTest {

  private final ProductScorer scorer = new ProductScorer();

  @ParameterizedTest(name = "{0}")
  @MethodSource("scoringScenarios")
  void shouldComputeWeightedScore(final double wSales, final double wStock, final double sSales,
                                  final double sStock, final double expected) {
    final var product = new ScoreableProduct(ProductId.of("1"), 100, 0.5);

    final List<SortingCriterion> criteria = List.of(
      new WeightedCriterion(s -> sSales, wSales),
      new WeightedCriterion(s -> sStock, wStock));

    final var score = scorer.compute(criteria, product);
    assertThat(score)
      .as("Weighted score should match expected")
      .isEqualTo(expected, offset(0.001));
  }

  private static Stream<Arguments> scoringScenarios() {
    return Stream.of(
      arguments(named("standard weights", 0.7), 0.3, 0.8, 0.5, 0.71),
      arguments(named("only sales", 1.0), 0.0, 0.8, 0.5, 0.80),
      arguments(named("only stock", 0.0), 1.0, 0.8, 0.5, 0.50));
  }
}
