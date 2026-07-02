package dev.jpje.productsorter.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import dev.jpje.productsorter.application.port.SortProductsRequest;
import dev.jpje.productsorter.domain.model.AppliedWeights;
import dev.jpje.productsorter.domain.model.Metrics;
import dev.jpje.productsorter.domain.model.Product;
import dev.jpje.productsorter.domain.port.ProductRepository;
import dev.jpje.productsorter.domain.vo.CursorCodec;
import dev.jpje.productsorter.domain.vo.ProductId;
import dev.jpje.productsorter.domain.vo.ProductName;
import dev.jpje.productsorter.domain.vo.SalesUnits;
import dev.jpje.productsorter.domain.vo.Size;
import dev.jpje.productsorter.domain.vo.Stock;
import dev.jpje.productsorter.domain.vo.StockBySize;
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
    useCase = new SortProductsUseCaseImpl(repository);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("sortScenarios")
  void shouldSortProductsByDescendingWeightedScore(final SortProductsRequest request,
                                                    final int productCount) {
    repository.products = IntStream.range(0, productCount)
      .mapToObj(i -> new Product(
        ProductId.of(String.valueOf(i + 1)),
        ProductName.of("Product " + (i + 1)),
        SalesUnits.of((i + 1) * 100),
        Stock.of(List.of(
          StockBySize.of(Size.of("S"), 1),
          StockBySize.of(Size.of("M"), 1),
          StockBySize.of(Size.of("L"), 1))))
      )
      .toList();

    final var result = useCase.execute(request, null, productCount);
    assertThat(result.products())
      .as("Should return all products")
      .hasSize(productCount);
    assertThat(result.products())
      .as("Should be sorted by weightedScore descending")
      .extracting(Product::weightedScore)
      .isSortedAccordingTo(Comparator.reverseOrder());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidRequests")
  void shouldRejectInvalidRequests(final Map<String, Double> weights) {
    assertThatThrownBy(() -> new SortProductsRequest(weights))
      .as("Invalid weights should throw")
      .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("emptyScenarios")
  void shouldReturnEmptyForInvalidPagination(final Integer size) {
    repository.products = List.of(new Product(
      ProductId.of("1"), ProductName.of("Test"),
      SalesUnits.of(100), Stock.of(List.of())));

    final var request = new SortProductsRequest(Map.of(
      Metrics.SALES_UNITS.key(), 0.7, Metrics.STOCK.key(), 0.3));
    final var result = useCase.execute(request, null, size);
    assertThat(result.products()).isEmpty();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("cursorScenarios")
  void shouldSupportCursorPagination(final int totalProducts, final int size, final int expectedFirstPage,
                                     final int expectedSecondPage) {
    repository.products = IntStream.range(0, totalProducts)
      .mapToObj(i -> new Product(
        ProductId.of(String.valueOf(i + 1)),
        ProductName.of("P" + (i + 1)),
        SalesUnits.of((totalProducts - i) * 100),
        Stock.of(List.of(
          StockBySize.of(Size.of("S"), 1), StockBySize.of(Size.of("M"), 1), StockBySize.of(Size.of("L"), 1)))))
      .toList();

    final var firstPage = useCase.execute(
      new SortProductsRequest(Map.of(Metrics.SALES_UNITS.key(), 1.0, Metrics.STOCK.key(), 0.0)),
      null, size);
    assertThat(firstPage.products()).hasSize(expectedFirstPage);

    if (firstPage.hasMore()) {
      final var secondPage = useCase.execute(
        new SortProductsRequest(Map.of(Metrics.SALES_UNITS.key(), 1.0, Metrics.STOCK.key(), 0.0)),
        firstPage.nextCursor(), size);
      assertThat(secondPage.products()).hasSize(expectedSecondPage);
    }
  }

  private static Stream<Arguments> sortScenarios() {
    return Stream.of(
      arguments(named("sales only", new SortProductsRequest(Map.of(
        Metrics.SALES_UNITS.key(), 1.0, Metrics.STOCK.key(), 0.0))), 6),
      arguments(named("stock only", new SortProductsRequest(Map.of(
        Metrics.SALES_UNITS.key(), 0.0, Metrics.STOCK.key(), 1.0))), 6),
      arguments(named("balanced", new SortProductsRequest(Map.of(
        Metrics.SALES_UNITS.key(), 0.7, Metrics.STOCK.key(), 0.3))), 6));
  }

  private static Stream<Arguments> invalidRequests() {
    return Stream.of(
      arguments(named("null", null)),
      arguments(named("empty", Map.of())),
      arguments(named("invalid weight", Map.of(Metrics.SALES_UNITS.key(), 1.5))));
  }

  private static Stream<Arguments> emptyScenarios() {
    return Stream.of(
      arguments(named("null size", (Integer) null)),
      arguments(named("zero size", 0)));
  }

  private static Stream<Arguments> cursorScenarios() {
    return Stream.of(
      arguments(named("6 products page 2", 6), 2, 2, 2),
      arguments(named("5 products page 3", 5), 3, 3, 2));
  }

  private static class TestProductRepository implements ProductRepository {

    List<Product> products = List.of();

    @Override
    public PagedResult findPage(final String cursor, final int limit) {
      var start = 0;
      if (cursor != null) {
        final var decoded = CursorCodec.decode(cursor);
        for (int i = 0; i < products.size(); i++) {
          if (products.get(i).productId().value().equals(decoded.productId())) {
            start = i + 1;
            break;
          }
        }
      }
      final var page = products.stream().skip(start).limit(limit).toList();
      final var nextCursor = page.size() == limit && !page.isEmpty()
        ? page.getLast().productId().value()
        : null;
      return new PagedResult(page, nextCursor);
    }

    @Override
    public PagedResult sortByWeights(final AppliedWeights weights, final String cursor, final int limit) {
      final var sorted = products.stream()
        .map(p -> {
          final var salesScore = weights.salesUnitsWeight() * p.salesUnits().value();
          final var stockScore = weights.stockWeight() * p.stock().stockRatio();
          final var weightedScore = salesScore + stockScore;
          return new Product(p.productId(), p.productName(), p.salesUnits(), p.stock(), weightedScore);
        })
        .sorted((a, b) -> Double.compare(
          b.weightedScore() != null ? b.weightedScore() : 0,
          a.weightedScore() != null ? a.weightedScore() : 0))
        .toList();

      var start = 0;
      if (cursor != null) {
        final var decoded = CursorCodec.decode(cursor);
        for (int i = 0; i < sorted.size(); i++) {
          if (sorted.get(i).productId().value().equals(decoded.productId())) {
            start = i + 1;
            break;
          }
        }
      }

      final var page = sorted.stream().skip(start).limit(limit).toList();
      final var pageSize = limit - 1;
      final var hasMore = page.size() > pageSize;
      final var nextCursor = hasMore
        ? page.get(pageSize - 1).productId().value()
        : null;

      return new PagedResult(page, nextCursor);
    }
  }
}
