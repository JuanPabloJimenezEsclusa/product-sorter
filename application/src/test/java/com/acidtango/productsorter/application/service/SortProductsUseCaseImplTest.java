package com.acidtango.productsorter.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.acidtango.productsorter.domain.model.Product;
import com.acidtango.productsorter.domain.port.ProductRepository;
import com.acidtango.productsorter.domain.service.ScoredProduct;
import com.acidtango.productsorter.domain.service.SortingEngine;
import com.acidtango.productsorter.domain.vo.ProductId;
import com.acidtango.productsorter.domain.vo.ProductName;
import com.acidtango.productsorter.domain.vo.SalesUnits;
import com.acidtango.productsorter.domain.vo.Size;
import com.acidtango.productsorter.domain.vo.Stock;
import com.acidtango.productsorter.domain.vo.StockBySize;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SortProductsUseCaseImplTest {

  private SortProductsUseCaseImpl useCase;
  private TestProductRepository repository;

  @BeforeEach
  void setUp() {
    repository = new TestProductRepository();
    useCase = new SortProductsUseCaseImpl(repository, new SortingEngine());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("validRequests")
  void shouldSortProductsViaRepository(final Map<String, Double> weights, final int productCount) {
    repository.products = IntStream.range(0, productCount)
      .mapToObj(i -> Instancio.of(Product.class)
        .set(field(Product::productId), ProductId.of((long) i + 1))
        .set(field(Product::productName), ProductName.of("Product " + (i + 1)))
        .set(field(Product::salesUnits), SalesUnits.of((i + 1) * 100))
        .set(field(Product::stock), Stock.of(List.of(
          StockBySize.of(Size.S, 1), StockBySize.of(Size.M, 1), StockBySize.of(Size.L, 1))))
        .create())
      .toList();

    final var result = useCase.execute(weights);
    assertThat(result)
      .as("Should return all products")
      .hasSize(productCount);
    assertThat(result)
      .as("Should be sorted by score descending")
      .extracting(ScoredProduct::score)
      .isSortedAccordingTo(Comparator.reverseOrder());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidRequests")
  void shouldRejectInvalidRequests(final Map<String, Double> weights) {
    assertThatThrownBy(() -> new SortProductsRequest(weights))
      .as("Invalid weights should throw")
      .isInstanceOf(IllegalArgumentException.class);
  }

  private static Stream<Arguments> validRequests() {
    return Stream.of(
      arguments(named("6 products", Map.of("salesUnits", 0.7, "stockRatio", 0.3)), 6),
      arguments(named("1 product", Map.of("salesUnits", 1.0, "stockRatio", 0.0)), 1));
  }

  private static Stream<Arguments> invalidRequests() {
    return Stream.of(
      arguments(named("null", null)),
      arguments(named("empty", Map.of())),
      arguments(named("invalid weight", Map.of("salesUnits", 1.5))));
  }

  private static class TestProductRepository implements ProductRepository {

    List<Product> products = List.of();

    @Override
    public List<Product> findAll() {
      return products;
    }
  }
}
