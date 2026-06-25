package dev.jpje.productsorter.adapter.rest.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import dev.jpje.productsorter.api.v1.dto.SortRequest;
import dev.jpje.productsorter.application.port.ListProductsUseCase;
import dev.jpje.productsorter.application.port.SortProductsUseCase;
import dev.jpje.productsorter.domain.model.Product;
import dev.jpje.productsorter.domain.port.ProductRepository.PagedResult;
import dev.jpje.productsorter.domain.vo.ProductId;
import dev.jpje.productsorter.domain.vo.ProductName;
import dev.jpje.productsorter.domain.vo.SalesUnits;
import dev.jpje.productsorter.domain.vo.Size;
import dev.jpje.productsorter.domain.vo.Stock;
import dev.jpje.productsorter.domain.vo.StockBySize;
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
    when(sortUseCase.execute(any(), isNull(), any()))
      .thenReturn(new PagedResult(List.of(), 0, null));

    final var dto = new SortRequest();
    dto.setWeights(Map.of("salesUnits", wSales, "stockRatio", wStock));

    final var response = controller.sortProducts(dto, null, null);
    assertThat(response.getStatusCode().value()).isEqualTo(200);
  }

  @Test
  void shouldReturnSortedProducts() {
    final var scoredProducts = IntStream.range(0, 3)
      .mapToObj(i -> new Product(
        ProductId.of(String.valueOf(i + 1)), ProductName.of("P" + (i + 1)),
        SalesUnits.of((i + 1) * 100),
        Stock.of(List.of(StockBySize.of(Size.S, 1), StockBySize.of(Size.M, 1), StockBySize.of(Size.L, 1))),
        (3 - i) * 0.1))
      .toList();
    when(sortUseCase.execute(any(), isNull(), any())).thenReturn(new PagedResult(scoredProducts, 3, null));

    final var dto = new SortRequest();
    dto.setWeights(Map.of("salesUnits", 0.7, "stockRatio", 0.3));

    final var response = controller.sortProducts(dto, null, null);
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getData()).hasSize(3);
    assertThat(response.getBody().getTotal()).isEqualTo(3);
  }

  @Test
  void shouldReturnProductsWithPagination() {
    final var products = List.of(
      new Product(ProductId.of("1"), ProductName.of("P1"), SalesUnits.of(100),
        Stock.of(List.of(StockBySize.of(Size.S, 1), StockBySize.of(Size.M, 1), StockBySize.of(Size.L, 1)))),
      new Product(ProductId.of("2"), ProductName.of("P2"), SalesUnits.of(50),
        Stock.of(List.of(StockBySize.of(Size.S, 0), StockBySize.of(Size.M, 0), StockBySize.of(Size.L, 0)))));
    when(listUseCase.execute(isNull(), eq(10))).thenReturn(new PagedResult(products, 2, null));

    final var response = controller.getProducts(null, 10);
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getData()).hasSize(2);
    assertThat(response.getBody().getSize()).isEqualTo(10);
  }

  @Test
  void shouldReturnEmptyPageWhenNoProducts() {
    when(listUseCase.execute(isNull(), eq(20))).thenReturn(new PagedResult(List.of(), 0, null));

    final var response = controller.getProducts(null, null);
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getData()).isEmpty();
  }
}
