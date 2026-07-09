package dev.jpje.productsorter.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import dev.jpje.productsorter.domain.model.AppliedWeights;
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

class ListProductsUseCaseImplTest {

  private ListProductsUseCaseImpl useCase;
  private TestProductRepository repository;

  @BeforeEach
  void setUp() {
    repository = new TestProductRepository();
    useCase = new ListProductsUseCaseImpl(repository);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("paginationScenarios")
  void shouldReturnPageCursorBased(final int totalProducts, final int size,
                                   final int expectedCount) {
    repository.products = products(totalProducts);

    final var result = useCase.execute(null, size);
    assertThat(result.products()).as("Should have %d products", expectedCount).hasSize(expectedCount);
    assertThat(result.nextCursor()).as("Should have nextCursor").isNotNull();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("cursorScenarios")
  void shouldPaginateWithCursor(final int totalProducts, final int size,
                                final int expectedFirst, final int expectedSecond) {
    repository.products = products(totalProducts);

    final var page1 = useCase.execute(null, size);
    assertThat(page1.products()).as("first page size").hasSize(expectedFirst);
    assertThat(page1.nextCursor()).as("first page has next cursor").isNotNull();

    final var page2 = useCase.execute(page1.nextCursor(), size);
    assertThat(page2.products()).as("second page size").hasSize(expectedSecond);
  }

  private static Product product(final String id, final int salesUnits) {
    return new Product(
      ProductId.of(id), ProductName.of("P" + id), SalesUnits.of(salesUnits),
      Stock.of(List.of(
        StockBySize.of(Size.of("S"), 1),
        StockBySize.of(Size.of("M"), 1),
        StockBySize.of(Size.of("L"), 1))));
  }

  private static List<Product> products(final int count) {
    return IntStream.range(0, count)
      .mapToObj(i -> product(String.valueOf(i + 1), (i + 1) * 100))
      .toList();
  }

  private static Stream<Arguments> paginationScenarios() {
    return Stream.of(
      arguments(named("3 products page 2", 3), 2, 2));
  }

  private static Stream<Arguments> cursorScenarios() {
    return Stream.of(
      arguments(named("4 products page 2", 4), 2, 2, 2),
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
      return new PagedResult(List.of(), null);
    }
  }
}
