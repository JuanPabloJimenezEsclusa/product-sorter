package dev.jpje.productsorter.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import dev.jpje.productsorter.domain.model.Product;
import dev.jpje.productsorter.domain.model.ScoreableProduct;
import dev.jpje.productsorter.domain.port.ProductRepository;
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
  void shouldReturnPage(final int totalProducts, final int page, final int size,
                        final int expectedCount, final String firstId) {
    repository.products = products(totalProducts);

    final var result = useCase.execute(page, size);
    assertThat(result)
      .as("Should have %d products", expectedCount)
      .hasSize(expectedCount);
    if (!result.isEmpty()) {
      assertThat(result.getFirst().productId().value())
        .as("First product ID should match")
        .isEqualTo(firstId);
    }
  }

  private static Stream<Arguments> paginationScenarios() {
    return Stream.of(
      arguments(named("first page", 3), 1, 2, 2, "1"),
      arguments(named("page out of range", 1), 99, 20, 0, null));
  }

  private static Product product(final String id, final int salesUnits) {
    return new Product(
      ProductId.of(id), ProductName.of("P" + id), SalesUnits.of(salesUnits),
      Stock.of(List.of(StockBySize.of(Size.S, 1), StockBySize.of(Size.M, 1), StockBySize.of(Size.L, 1))));
  }

  private static List<Product> products(final int count) {
    return IntStream.range(0, count)
      .mapToObj(i -> product(String.valueOf(i + 1), (i + 1) * 100))
      .toList();
  }

  private static class TestProductRepository implements ProductRepository {
    List<Product> products = List.of();

    @Override
    public List<Product> findPage(final int page, final int size) {
      if (page < 1 || size < 1) {
        return List.of();
      }
      final var skip = ((long) page - 1) * size;
      return products.stream().skip(skip).limit(size).toList();
    }

    @Override
    public java.util.OptionalInt findMaxSalesUnits() {
      return java.util.OptionalInt.empty();
    }

    @Override
    public List<ScoreableProduct> findAllScoreable() {
      return List.of();
    }

    @Override
    public List<Product> findByIds(final List<dev.jpje.productsorter.domain.vo.ProductId> ids) {
      return List.of();
    }
  }
}
