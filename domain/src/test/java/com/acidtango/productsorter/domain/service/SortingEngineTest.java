package com.acidtango.productsorter.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.acidtango.productsorter.domain.model.ScoreableProduct;
import com.acidtango.productsorter.domain.service.SortingEngine.ScoredScoreable;
import com.acidtango.productsorter.domain.vo.ProductId;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SortingEngineTest {

  private final SortingEngine engine = new SortingEngine();

  @ParameterizedTest(name = "{0}")
  @MethodSource("sortingScenarios")
  void shouldSortProductsByDescendingWeightedScore(final Map<String, Double> weights) {
    final var products = List.of(
      new ScoreableProduct(ProductId.of(1L), 650, 0.333),
      new ScoreableProduct(ProductId.of(2L), 50, 1.0),
      new ScoreableProduct(ProductId.of(3L), 80, 1.0),
      new ScoreableProduct(ProductId.of(4L), 3, 1.0),
      new ScoreableProduct(ProductId.of(5L), 650, 0.333),
      new ScoreableProduct(ProductId.of(6L), 20, 1.0));

    final var sorted = engine.sortScoreables(products, weights, 650);
    assertThat(sorted)
      .as("Should return all products")
      .hasSize(6);
    assertThat(sorted)
      .as("Should be sorted by score descending")
      .extracting(ScoredScoreable::score)
      .isSortedAccordingTo(Comparator.reverseOrder());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("edgeCases")
  void shouldHandleEdgeCases(final List<ScoreableProduct> products,
                             final Map<String, Double> weights,
                             final int expectedSize) {
    final var sorted = engine.sortScoreables(products, weights, 1);
    assertThat(sorted)
      .as("Should return correct number of products")
      .hasSize(expectedSize);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("existingProductScenarios")
  void shouldSortProductsAsSpecifiedInDocument(final List<ScoreableProduct> products,
                                               final Map<String, Double> weights,
                                               final int maxSales,
                                               final long firstProductId,
                                               final long lastProductId) {
    final var sorted = engine.sortScoreables(products, weights, maxSales);
    final var ids = sorted.stream().map(s -> s.product().productId().value()).toList();
    assertThat(ids.getFirst())
      .as("First product should match expected")
      .isEqualTo(firstProductId);
    assertThat(ids.getLast())
      .as("Last product should match expected")
      .isEqualTo(lastProductId);
  }

  private static Stream<Arguments> sortingScenarios() {
    return Stream.of(
      arguments(named("sales only", Map.of("salesUnits", 1.0, "stockRatio", 0.0))),
      arguments(named("stock only", Map.of("salesUnits", 0.0, "stockRatio", 1.0))),
      arguments(named("balanced", Map.of("salesUnits", 0.5, "stockRatio", 0.5))));
  }

  private static Stream<Arguments> edgeCases() {
    return Stream.of(
      arguments(named("empty products", List.of()), Map.of("salesUnits", 1.0, "stockRatio", 0.0), 0));
  }

  private static Stream<Arguments> existingProductScenarios() {
    final var existingProducts = List.of(
      new ScoreableProduct(ProductId.of(1L), 100, 0.667),
      new ScoreableProduct(ProductId.of(2L), 50, 1.0),
      new ScoreableProduct(ProductId.of(3L), 80, 1.0),
      new ScoreableProduct(ProductId.of(4L), 3, 1.0),
      new ScoreableProduct(ProductId.of(5L), 650, 0.333),
      new ScoreableProduct(ProductId.of(6L), 20, 1.0));
    return Stream.of(
      arguments(named("sales desc", existingProducts), Map.of("salesUnits", 1.0, "stockRatio", 0.0), 650, 5L, 4L),
      arguments(named("stock desc", existingProducts), Map.of("salesUnits", 0.0, "stockRatio", 1.0), 650, 2L, 5L),
      arguments(named("mixed", existingProducts), Map.of("salesUnits", 0.7, "stockRatio", 0.3), 650, 5L, 4L));
  }
}
