package dev.jpje.productsorter.adapter.observability;

import java.time.Duration;

import dev.jpje.productsorter.application.port.SortProductsRequest;
import dev.jpje.productsorter.application.port.SortProductsUseCase;
import dev.jpje.productsorter.domain.port.ProductRepository.PagedResult;

public class MetricsSortProductsUseCase implements SortProductsUseCase {

  private final SortProductsUseCase delegate;
  private final SortingMetrics metrics;

  public MetricsSortProductsUseCase(final SortProductsUseCase delegate, final SortingMetrics metrics) {
    this.delegate = delegate;
    this.metrics = metrics;
  }

  @Override
  public PagedResult execute(final SortProductsRequest request, final String cursor, final Integer size) {
    final var start = System.nanoTime();
    final var result = delegate.execute(request, cursor, size);
    final var elapsed = Duration.ofNanos(System.nanoTime() - start);
    metrics.recordSort(result.products().size(), elapsed, request.weights());
    return result;
  }
}
