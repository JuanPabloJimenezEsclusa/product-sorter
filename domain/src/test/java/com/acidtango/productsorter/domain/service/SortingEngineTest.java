package com.acidtango.productsorter.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.acidtango.productsorter.domain.model.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

class SortingEngineTest {

  private final SortingEngine engine = new SortingEngine();

  @ParameterizedTest
  @MethodSource("sortingScenarios")
  void shouldSortProductsByDescendingWeightedScore(final Map<String, Double> weights) {
    final var products = List.of(
      product(1L, 650, StockMother.from("S:0,M:1,L:0")),
      product(2L, 50, StockMother.from("S:35,M:9,L:9")),
      product(3L, 80, StockMother.from("S:20,M:2,L:20")),
      product(4L, 3, StockMother.from("S:25,M:30,L:10")),
      product(5L, 650, StockMother.from("S:0,M:1,L:0")),
      product(6L, 20, StockMother.from("S:9,M:2,L:5")));

    final var sorted = engine.sort(products, weights);
    assertThat(sorted)
      .as("Should return all products")
      .hasSize(6);
    assertThat(sorted)
      .as("Should be sorted by score descending")
      .extracting(ScoredProduct::score)
      .isSortedAccordingTo(Comparator.reverseOrder());
  }

  @ParameterizedTest
  @MethodSource("edgeCases")
  void shouldHandleEdgeCases(final List<Product> products, final Map<String, Double> weights,
                              final int expectedSize) {
    final var sorted = engine.sort(products, weights);
    assertThat(sorted)
      .as("Should return correct number of products")
      .hasSize(expectedSize);
  }

  @ParameterizedTest
  @MethodSource("existingProductScenarios")
  void shouldSortProductsAsSpecifiedInDocument(final List<Product> products,
                                                final Map<String, Double> weights,
                                                final long firstProductId,
                                                final long lastProductId) {
    final var sorted = engine.sort(products, weights);
    final var ids = sorted.stream().map(sp -> sp.product().productId().value()).toList();
    assertThat(ids.getFirst())
      .as("First product should match expected")
      .isEqualTo(firstProductId);
    assertThat(ids.getLast())
      .as("Last product should match expected")
      .isEqualTo(lastProductId);
  }

  private static Stream<Arguments> existingProductScenarios() {
    final var existingProducts = List.of(
      product(1L, 100, StockMother.from("S:4,M:9,L:0")),
      product(2L, 50, StockMother.from("S:35,M:9,L:9")),
      product(3L, 80, StockMother.from("S:20,M:2,L:20")),
      product(4L, 3, StockMother.from("S:25,M:30,L:10")),
      product(5L, 650, StockMother.from("S:0,M:1,L:0")),
      product(6L, 20, StockMother.from("S:9,M:2,L:5")));
    return Stream.of(
      Arguments.of(existingProducts, Map.of("salesUnits", 1.0, "stockRatio", 0.0), 5L, 4L),
      Arguments.of(existingProducts, Map.of("salesUnits", 0.0, "stockRatio", 1.0), 2L, 5L),
      Arguments.of(existingProducts, Map.of("salesUnits", 0.7, "stockRatio", 0.3), 5L, 4L));
  }

  private static Stream<Arguments> sortingScenarios() {
    return Stream.of(
      Arguments.of(Map.of("salesUnits", 1.0, "stockRatio", 0.0)),
      Arguments.of(Map.of("salesUnits", 0.0, "stockRatio", 1.0)),
      Arguments.of(Map.of("salesUnits", 0.5, "stockRatio", 0.5)));
  }

  private static Stream<Arguments> edgeCases() {
    return Stream.of(
      Arguments.of(List.of(), Map.of("salesUnits", 1.0, "stockRatio", 0.0), 0));
  }

  private static Product product(final long id, final int salesUnits, final Stock stock) {
    return new Product(
      ProductId.of(id),
      ProductName.of("Product " + id),
      SalesUnits.of(salesUnits),
      stock);
  }
}
