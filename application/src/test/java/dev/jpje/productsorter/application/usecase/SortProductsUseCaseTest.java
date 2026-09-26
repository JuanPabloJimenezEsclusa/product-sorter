package dev.jpje.productsorter.application.usecase;

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
import dev.jpje.productsorter.domain.port.ProductPage;
import dev.jpje.productsorter.domain.port.ProductRepository;
import dev.jpje.productsorter.domain.vo.CursorCodec;
import dev.jpje.productsorter.domain.vo.ProductId;
import dev.jpje.productsorter.domain.vo.ProductName;
import dev.jpje.productsorter.domain.vo.SalesUnits;
import dev.jpje.productsorter.domain.vo.Size;
import dev.jpje.productsorter.domain.vo.Stock;
import dev.jpje.productsorter.domain.vo.StockBySize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SortProductsUseCaseTest {

  private static final double MIDPOINT = 50.0;

  private SortProductsUseCase useCase;
  private TestProductRepository repository;

  @BeforeEach
  void setUp() {
    repository = new TestProductRepository();
    useCase = new SortProductsUseCase(repository);
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

  @Test
  void shouldDefaultSizeWhenAbsent() {
    repository.products = singleProduct();
    final var request = new SortProductsRequest(Map.of(
      Metrics.SALES_UNITS.key(), 0.7, Metrics.STOCK.key(), 0.3));

    final var result = useCase.execute(request, null, null);

    assertThat(result.products()).as("products returned with the default size").hasSize(1);
    assertThat(result.size()).as("absent size resolves to the default").isEqualTo(20);
  }

  @Test
  void shouldRejectOutOfRangeSize() {
    repository.products = singleProduct();
    final var request = new SortProductsRequest(Map.of(
      Metrics.SALES_UNITS.key(), 0.7, Metrics.STOCK.key(), 0.3));

    assertThatThrownBy(() -> useCase.execute(request, null, 0))
      .as("a size below the minimum is rejected, not clamped")
      .isInstanceOf(IllegalArgumentException.class);
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
    assertThat(firstPage.products()).as("first page size").hasSize(expectedFirstPage);

    final var secondPage = useCase.execute(
      new SortProductsRequest(Map.of(Metrics.SALES_UNITS.key(), 1.0, Metrics.STOCK.key(), 0.0)),
      firstPage.nextCursor(), size);
    assertThat(secondPage.products()).as("second page size").hasSize(expectedSecondPage);
  }

  private static List<Product> singleProduct() {
    return List.of(new Product(
      ProductId.of("1"), ProductName.of("Test"), SalesUnits.of(100), Stock.of(List.of())));
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

  private static Stream<Arguments> cursorScenarios() {
    return Stream.of(
      arguments(named("6 products page 2", 6), 2, 2, 2),
      arguments(named("5 products page 3", 5), 3, 3, 2));
  }

  private static class TestProductRepository implements ProductRepository {

    List<Product> products = List.of();

    @Override
    public ProductPage findPage(final String cursor, final int limit) {
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
      return new ProductPage(products.stream().skip(start).limit(limit).toList());
    }

    @Override
    public ProductPage sortByWeights(final AppliedWeights weights, final String cursor, final int limit) {
      final var sorted = products.stream()
        .map(p -> new Product(p.productId(), p.productName(), p.salesUnits(), p.stock(),
          weights.computeScore(p.salesUnits(), p.stock(), MIDPOINT)))
        .sorted((a, b) -> Double.compare(score(b), score(a)))
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

      return new ProductPage(sorted.stream().skip(start).limit(limit).toList());
    }

    private static double score(final Product product) {
      return product.weightedScore() != null ? product.weightedScore() : 0;
    }
  }
}
