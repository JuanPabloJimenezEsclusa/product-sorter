package com.acidtango.productsorter.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acidtango.productsorter.domain.model.*;
import com.acidtango.productsorter.domain.port.ProductRepository;
import com.acidtango.productsorter.domain.service.ScoredProduct;
import com.acidtango.productsorter.domain.service.SortingEngine;
import org.instancio.Instancio;
import static org.instancio.Select.field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class SortProductsUseCaseImplTest {

  private SortProductsUseCaseImpl useCase;
  private TestProductRepository repository;

  @BeforeEach
  void setUp() {
    repository = new TestProductRepository();
    useCase = new SortProductsUseCaseImpl(repository, new SortingEngine());
  }

  @ParameterizedTest
  @MethodSource("validRequests")
  void shouldSortProductsViaRepository(final Map<String, Double> weights, final int productCount) {
    final var products = IntStream.range(0, productCount)
      .mapToObj(i -> Instancio.of(Product.class)
        .set(field(Product::productId), ProductId.of((long) i + 1))
        .set(field(Product::productName), ProductName.of("Product " + (i + 1)))
        .set(field(Product::salesUnits), SalesUnits.of((i + 1) * 100))
        .set(field(Product::stock), Stock.of(List.of(
          StockBySize.of(Size.S, 1), StockBySize.of(Size.M, 1), StockBySize.of(Size.L, 1))))
        .create())
      .toList();
    repository.products = products;

    final var result = useCase.execute(weights);
    assertThat(result)
      .as("Should return all products")
      .hasSize(productCount);
    assertThat(result)
      .as("Should be sorted by score descending")
      .extracting(ScoredProduct::score)
      .isSortedAccordingTo(Comparator.reverseOrder());
  }

  @ParameterizedTest
  @MethodSource("invalidRequests")
  void shouldRejectInvalidRequests(final Map<String, Double> weights) {
    assertThatThrownBy(() -> new SortProductsRequest(weights))
      .as("Invalid weights should throw")
      .isInstanceOf(IllegalArgumentException.class);
  }

  private static Stream<Arguments> validRequests() {
    return Stream.of(
      Arguments.of(Map.of("salesUnits", 0.7, "stockRatio", 0.3), 6),
      Arguments.of(Map.of("salesUnits", 1.0, "stockRatio", 0.0), 1));
  }

  private static Stream<Arguments> invalidRequests() {
    return Stream.of(
      Arguments.of((Object) null),
      Arguments.of(Map.of()),
      Arguments.of(Map.of("salesUnits", 1.5)));
  }

  private static class TestProductRepository implements ProductRepository {

    List<Product> products = List.of();

    @Override
    public List<Product> findAll() {
      return products;
    }
  }
}
