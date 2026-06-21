package com.acidtango.productsorter.adapter.rest.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.acidtango.productsorter.domain.model.*;
import com.acidtango.productsorter.domain.port.SortProductsUseCase;
import com.acidtango.productsorter.domain.service.ScoredProduct;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductControllerTest {

  private final SortProductsUseCase useCase = mock(SortProductsUseCase.class);
  private final ProductController controller = new ProductController(useCase);

  @ParameterizedTest
  @CsvSource({
    "0.7, 0.3",
    "1.0, 0.0",
    "0.0, 1.0"
  })
  void shouldReturn200ForValidWeights(final Double wSales, final Double wStock) {
    when(useCase.execute(any())).thenReturn(List.of());

    final var dto = new com.acidtango.productsorter.api.v1.dto.SortRequest();
    dto.setWeights(Map.of("salesUnits", wSales, "stockRatio", wStock));

    final var response = controller.sortProducts(dto);

    assertThat(response.getStatusCode().value())
      .as("Should return HTTP 200 for valid weights")
      .isEqualTo(200);
  }

  @Test
  void shouldReturnSortedProducts() {
    final var scoredProducts = IntStream.range(0, 3)
      .mapToObj(i -> new ScoredProduct(
        new Product(ProductId.of((long) i + 1), ProductName.of("P" + (i + 1)),
          SalesUnits.of((i + 1) * 100),
          Stock.of(List.of(StockBySize.of(Size.S, 1), StockBySize.of(Size.M, 1), StockBySize.of(Size.L, 1)))),
        (3 - i) * 0.1))
      .toList();
    when(useCase.execute(any())).thenReturn(scoredProducts);

    final var dto = new com.acidtango.productsorter.api.v1.dto.SortRequest();
    dto.setWeights(Map.of("salesUnits", 0.7, "stockRatio", 0.3));

    final var response = controller.sortProducts(dto);

    assertThat(response.getStatusCode().value())
      .as("Should return HTTP 200")
      .isEqualTo(200);
    assertThat(response.getBody())
      .as("Response body should not be null")
      .isNotNull();
    assertThat(response.getBody().getSortedProducts())
      .as("Should contain 3 sorted products")
      .hasSize(3);
  }
}
