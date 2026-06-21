package dev.jpje.productsorter.adapter.observability;

import java.time.Duration;
import java.util.List;

import dev.jpje.productsorter.application.port.SortProductsRequest;
import dev.jpje.productsorter.application.port.SortProductsUseCase;
import dev.jpje.productsorter.domain.service.ScoredProduct;

public class MetricsSortProductsUseCase implements SortProductsUseCase {

  private final SortProductsUseCase delegate;
  private final SortingMetrics metrics;

  public MetricsSortProductsUseCase(final SortProductsUseCase delegate, final SortingMetrics metrics) {
    this.delegate = delegate;
    this.metrics = metrics;
  }

  @Override
  public List<ScoredProduct> execute(final SortProductsRequest request, final int page, final int size) {
    final var start = System.nanoTime();
    final var result = delegate.execute(request, page, size);
    final var elapsed = Duration.ofNanos(System.nanoTime() - start);
    metrics.recordSort(result.size(), elapsed, request.weights());
    return result;
  }
}
