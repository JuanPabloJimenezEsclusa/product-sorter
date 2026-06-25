package dev.jpje.productsorter.adapter.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import dev.jpje.productsorter.application.port.SortProducts;
import dev.jpje.productsorter.application.port.SortProductsRequest;
import dev.jpje.productsorter.domain.model.Metrics;
import dev.jpje.productsorter.domain.model.Product;
import dev.jpje.productsorter.domain.port.ProductRepository.PagedResult;
import dev.jpje.productsorter.domain.vo.SalesUnits;
import dev.jpje.productsorter.domain.vo.Stock;
import org.instancio.Instancio;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SortProductsMetricsDecoratorTest {

  @Mock
  private SortProducts delegate;

  @Mock
  private SortingMetrics metrics;

  @InjectMocks
  private SortProductsMetricsDecorator decorator;

  @ParameterizedTest(name = "{0}")
  @MethodSource("executeCases")
  void shouldDelegateAndRecordMetrics(final PagedResult delegateResult, final SortProductsRequest request) {
    when(delegate.execute(request, null, 20)).thenReturn(delegateResult);

    final var result = decorator.execute(request, null, 20);

    assertThat(result)
      .as("Should return delegate result")
      .isSameAs(delegateResult);
    verify(metrics).recordSort(eq(delegateResult.products().size()), any(Duration.class), eq(request.weights()));
  }

  private static Stream<Arguments> executeCases() {
    return Stream.of(
      arguments(named("empty result", new PagedResult(List.of(), null)),
        new SortProductsRequest(Map.of(Metrics.SALES_UNITS.key(), 0.7, Metrics.STOCK.key(), 0.3))),
      arguments(named("with products",
        new PagedResult(List.of(
          instancioProduct(100, 0.9),
          instancioProduct(50, 0.5)), null)),
        new SortProductsRequest(Map.of(Metrics.SALES_UNITS.key(), 0.7, Metrics.STOCK.key(), 0.3))));
  }

  private static Product instancioProduct(final int salesUnits, final double score) {
    return new Product(
      Instancio.of(Product.class)
        .set(field(Product::salesUnits), SalesUnits.of(salesUnits))
        .set(field(Product::stock), Stock.of(List.of()))
        .create()
        .productId(),
      Instancio.of(Product.class)
        .set(field(Product::salesUnits), SalesUnits.of(salesUnits))
        .set(field(Product::stock), Stock.of(List.of()))
        .create()
        .productName(),
      SalesUnits.of(salesUnits),
      Stock.of(List.of()),
      score);
  }
}
