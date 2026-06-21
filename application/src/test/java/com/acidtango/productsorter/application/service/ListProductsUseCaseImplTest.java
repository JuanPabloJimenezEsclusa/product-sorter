package com.acidtango.productsorter.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.acidtango.productsorter.domain.model.Product;
import com.acidtango.productsorter.domain.port.ProductRepository;
import com.acidtango.productsorter.domain.port.SortProductsUseCase;
import com.acidtango.productsorter.domain.service.SortingEngine;
import com.acidtango.productsorter.domain.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class ListProductsUseCaseImplTest {

  private ListProductsUseCaseImpl useCase;
  private TestProductRepository repository;

  @BeforeEach
  void setUp() {
    repository = new TestProductRepository();
    useCase = new ListProductsUseCaseImpl(repository);
  }

  @Test
  void shouldReturnAllProductsPaginated() {
    repository.products = List.of(
      product(1L, 100), product(2L, 50), product(3L, 80));

    final var result = useCase.execute(1, 2);
    assertThat(result.products()).as("Should return first page").hasSize(2);
    assertThat(result.total()).as("Should report total 3").isEqualTo(3);
    assertThat(result.totalPages()).as("Should report 2 pages").isEqualTo(2);
  }

  @Test
  void shouldReturnEmptyForPageOutOfRange() {
    repository.products = List.of(product(1L, 100));

    final var result = useCase.execute(99, 20);
    assertThat(result.products()).as("Should be empty").isEmpty();
    assertThat(result.total()).as("Should report total 1").isEqualTo(1);
  }

  private static Product product(final long id, final int salesUnits) {
    return new Product(
      ProductId.of(id), ProductName.of("P" + id), SalesUnits.of(salesUnits),
      Stock.of(List.of(StockBySize.of(Size.S, 1), StockBySize.of(Size.M, 1), StockBySize.of(Size.L, 1))));
  }

  private static class TestProductRepository implements ProductRepository {
    List<Product> products = List.of();

    @Override
    public List<Product> findAll() {
      return products;
    }
  }
}
