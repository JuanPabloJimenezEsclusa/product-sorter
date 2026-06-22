package com.acidtango.productsorter.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.acidtango.productsorter.domain.model.Product;
import com.acidtango.productsorter.domain.model.ScoreableProduct;
import com.acidtango.productsorter.domain.port.ProductRepository;
import com.acidtango.productsorter.domain.vo.ProductId;
import com.acidtango.productsorter.domain.vo.ProductName;
import com.acidtango.productsorter.domain.vo.SalesUnits;
import com.acidtango.productsorter.domain.vo.Size;
import com.acidtango.productsorter.domain.vo.Stock;
import com.acidtango.productsorter.domain.vo.StockBySize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListProductsUseCaseImplTest {

  private ListProductsUseCaseImpl useCase;
  private TestProductRepository repository;

  @BeforeEach
  void setUp() {
    repository = new TestProductRepository();
    useCase = new ListProductsUseCaseImpl(repository);
  }

  @Test
  void shouldReturnFirstPage() {
    repository.products = List.of(
      product("1", 100), product("2", 50), product("3", 80));

    final var result = useCase.execute(1, 2);
    assertThat(result).as("Should return first page").hasSize(2);
  }

  @Test
  void shouldReturnEmptyForPageOutOfRange() {
    repository.products = List.of(product("1", 100));

    final var result = useCase.execute(99, 20);
    assertThat(result).as("Should be empty").isEmpty();
  }

  @Test
  void shouldDelegatePaginationToRepository() {
    repository.products = List.of(
      product("1", 100), product("2", 50), product("3", 80), product("4", 200));

    final var page1 = useCase.execute(1, 2);
    assertThat(page1).as("Page 1 should have 2 products").hasSize(2);
    assertThat(page1.getFirst().productId().value()).as("First product").isEqualTo("1");

    final var page2 = useCase.execute(2, 2);
    assertThat(page2).as("Page 2 should have 2 products").hasSize(2);
    assertThat(page2.getFirst().productId().value()).as("First product on page 2").isEqualTo("3");

    final var page3 = useCase.execute(3, 2);
    assertThat(page3).as("Page 3 should be empty").isEmpty();
  }

  private static Product product(final String id, final int salesUnits) {
    return new Product(
      ProductId.of(id), ProductName.of("P" + id), SalesUnits.of(salesUnits),
      Stock.of(List.of(StockBySize.of(Size.S, 1), StockBySize.of(Size.M, 1), StockBySize.of(Size.L, 1))));
  }

  private static class TestProductRepository implements ProductRepository {
    List<Product> products = List.of();

    @Override
    public List<Product> findPage(final int page, final int size) {
      final var skip = (long) (page - 1) * size;
      return products.stream().skip(skip).limit(size).toList();
    }

    @Override
    public java.util.OptionalInt findMaxSalesUnits() { return java.util.OptionalInt.empty(); }

    @Override
    public List<ScoreableProduct> findAllScoreable() { return List.of(); }

    @Override
    public List<Product> findByIds(final List<com.acidtango.productsorter.domain.vo.ProductId> ids) { return List.of(); }
  }
}
