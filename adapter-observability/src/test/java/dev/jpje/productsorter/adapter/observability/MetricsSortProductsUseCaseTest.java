package dev.jpje.productsorter.adapter.observability;

import static dev.jpje.productsorter.domain.vo.CriterionType.SALES_UNITS;
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

import dev.jpje.productsorter.application.port.SortProductsRequest;
import dev.jpje.productsorter.application.port.SortProductsUseCase;
import dev.jpje.productsorter.domain.model.Product;
import dev.jpje.productsorter.domain.service.ScoredProduct;
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
class MetricsSortProductsUseCaseTest {

  @Mock
  private SortProductsUseCase delegate;

  @Mock
  private SortingMetrics metrics;

  @InjectMocks
  private MetricsSortProductsUseCase useCase;

  @ParameterizedTest(name = "{0}")
  @MethodSource("executeCases")
  void shouldDelegateAndRecordMetrics(final List<ScoredProduct> delegateResult, final SortProductsRequest request) {
    when(delegate.execute(request, 1, 20)).thenReturn(delegateResult);

    final var result = useCase.execute(request, 1, 20);

    assertThat(result)
      .as("Should return delegate result")
      .isSameAs(delegateResult);
    verify(metrics).recordSort(eq(delegateResult.size()), any(Duration.class), eq(request.weights()));
  }

  private static Stream<Arguments> executeCases() {
    return Stream.of(
      arguments(named("empty result", List.of()), new SortProductsRequest(Map.of(SALES_UNITS.key(), 0.7, "stockRatio", 0.3))),
      arguments(named("with products",
        List.of(
          new ScoredProduct(instancioProduct(100), 0.9),
          new ScoredProduct(instancioProduct(50), 0.5))),
        new SortProductsRequest(Map.of(SALES_UNITS.key(), 0.7, "stockRatio", 0.3))));
  }

  private static Product instancioProduct(final int salesUnits) {
    return Instancio.of(Product.class)
      .set(field(Product::salesUnits), SalesUnits.of(salesUnits))
      .set(field(Product::stock), Stock.of(List.of()))
      .create();
  }
}
