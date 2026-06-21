package com.acidtango.productsorter.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.stream.Stream;

import com.acidtango.productsorter.domain.model.Product;
import com.acidtango.productsorter.domain.model.StockMother;
import com.acidtango.productsorter.domain.vo.ProductId;
import com.acidtango.productsorter.domain.vo.ProductName;
import com.acidtango.productsorter.domain.vo.SalesUnits;
import org.instancio.Instancio;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ProductScorerTest {

  private final ProductScorer scorer = new ProductScorer();

  private static Stream<Arguments> scoringScenarios() {
    return Stream.of(
      arguments(named("standard weights", 0.7), 0.3, 0.8, 0.5, 0.71),
      arguments(named("only sales", 1.0), 0.0, 0.8, 0.5, 0.80),
      arguments(named("only stock", 0.0), 1.0, 0.8, 0.5, 0.50));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("scoringScenarios")
  void shouldComputeWeightedScore(final double wSales, final double wStock, final double sSales,
                                  final double sStock, final double expected) {
    final var product = Instancio.of(Product.class)
      .set(field(Product::salesUnits), SalesUnits.of(100))
      .set(field(Product::productId), ProductId.of(1L))
      .set(field(Product::productName), ProductName.of("Test"))
      .set(field(Product::stock), StockMother.from("S:1,M:1,L:1"))
      .create();

    final List<SortingCriterion> criteria = List.of(
      new WeightedCriterion(s -> sSales, wSales),
      new WeightedCriterion(s -> sStock, wStock));

    final var score = scorer.compute(criteria, product);
    assertThat(score)
      .as("Weighted score should match expected")
      .isEqualTo(expected, offset(0.001));
  }
}
