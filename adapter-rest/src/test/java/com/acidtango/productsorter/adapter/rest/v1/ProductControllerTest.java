package com.acidtango.productsorter.adapter.rest.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import com.acidtango.productsorter.api.v1.dto.SortRequest;
import com.acidtango.productsorter.domain.model.Product;
import com.acidtango.productsorter.domain.port.ListProductsUseCase;
import com.acidtango.productsorter.domain.port.SortProductsUseCase;
import com.acidtango.productsorter.domain.service.ScoredProductPage;
import com.acidtango.productsorter.domain.vo.ProductId;
import com.acidtango.productsorter.domain.vo.ProductName;
import com.acidtango.productsorter.domain.vo.SalesUnits;
import com.acidtango.productsorter.domain.vo.Size;
import com.acidtango.productsorter.domain.vo.Stock;
import com.acidtango.productsorter.domain.vo.StockBySize;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

  @Spy
  private ProductControllerMapper mapper;

  @Mock
  private SortProductsUseCase sortUseCase;

  @Mock
  private ListProductsUseCase listUseCase;

  @InjectMocks
  private ProductController controller;

  @ParameterizedTest
  @CsvSource({
    "0.7, 0.3",
    "1.0, 0.0",
    "0.0, 1.0"
  })
  void shouldReturn200ForValidWeights(final Double wSales, final Double wStock) {
    when(sortUseCase.execute(any(), eq(1), eq(20)))
      .thenReturn(ScoredProductPage.of(List.of(), 1, 20, 0));

    final var dto = new SortRequest();
    dto.setWeights(Map.of("salesUnits", wSales, "stockRatio", wStock));

    final var response = controller.sortProducts(dto, null, null);
    assertThat(response.getStatusCode().value()).as("Should return HTTP 200").isEqualTo(200);
  }

  @Test
  void shouldReturnSortedProducts() {
    final var scoredProducts = IntStream.range(0, 3)
      .mapToObj(i -> new com.acidtango.productsorter.domain.service.ScoredProduct(
        new Product(ProductId.of((long) i + 1), ProductName.of("P" + (i + 1)),
          SalesUnits.of((i + 1) * 100),
          Stock.of(List.of(StockBySize.of(Size.S, 1), StockBySize.of(Size.M, 1), StockBySize.of(Size.L, 1)))),
        (3 - i) * 0.1))
      .toList();
    final var domainPage = ScoredProductPage.of(scoredProducts, 1, 20, 3);
    when(sortUseCase.execute(any(), eq(1), eq(20))).thenReturn(domainPage);

    final var dto = new SortRequest();
    dto.setWeights(Map.of("salesUnits", 0.7, "stockRatio", 0.3));

    final var response = controller.sortProducts(dto, null, null);
    assertThat(response.getStatusCode().value()).as("Should return HTTP 200").isEqualTo(200);
    assertThat(response.getBody()).as("Response body should not be null").isNotNull();
    assertThat(response.getBody().getData()).as("Should contain 3 products").hasSize(3);
    assertThat(response.getBody().getPage()).as("Should report page 1").isEqualTo(1);
  }

  @Test
  void shouldReturnProductsWithPagination() {
    final var products = List.of(
      new Product(ProductId.of(1L), ProductName.of("P1"), SalesUnits.of(100),
        Stock.of(List.of(StockBySize.of(Size.S, 1), StockBySize.of(Size.M, 1), StockBySize.of(Size.L, 1)))),
      new Product(ProductId.of(2L), ProductName.of("P2"), SalesUnits.of(50),
        Stock.of(List.of(StockBySize.of(Size.S, 0), StockBySize.of(Size.M, 0), StockBySize.of(Size.L, 0)))));
    when(listUseCase.execute(1, 10)).thenReturn(com.acidtango.productsorter.domain.model.ProductPage.of(products, 1, 10, 2));

    final var response = controller.getProducts(1, 10);
    assertThat(response.getStatusCode().value()).as("Should return HTTP 200").isEqualTo(200);
    assertThat(response.getBody()).as("Body should not be null").isNotNull();
    assertThat(response.getBody().getData()).as("Should return 2 products").hasSize(2);
    assertThat(response.getBody().getTotal()).as("Should report total 2").isEqualTo(2);
  }

  @Test
  void shouldReturnEmptyPageWhenNoProducts() {
    when(listUseCase.execute(1, 20)).thenReturn(com.acidtango.productsorter.domain.model.ProductPage.of(List.of(), 1, 20, 0));

    final var response = controller.getProducts(null, null);
    assertThat(response.getStatusCode().value()).as("Should return HTTP 200").isEqualTo(200);
    assertThat(response.getBody()).as("Body should not be null").isNotNull();
    assertThat(response.getBody().getData()).as("Should be empty").isEmpty();
    assertThat(response.getBody().getTotal()).as("Should report 0 total").isEqualTo(0);
  }
}
