package com.acidtango.productsorter.infrastructure.observability;

import com.acidtango.productsorter.domain.service.ScoredProductPage;
import com.acidtango.productsorter.domain.port.SortProductsUseCase;

import java.time.Duration;
import java.util.Map;

public class MetricsSortProductsUseCase implements SortProductsUseCase {

  private final SortProductsUseCase delegate;
  private final SortingMetrics metrics;

  public MetricsSortProductsUseCase(final SortProductsUseCase delegate, final SortingMetrics metrics) {
    this.delegate = delegate;
    this.metrics = metrics;
  }

  @Override
  public ScoredProductPage execute(final Map<String, Double> weights, final int page, final int size) {
    final var start = System.nanoTime();
    final var result = delegate.execute(weights, page, size);
    final var elapsed = Duration.ofNanos(System.nanoTime() - start);
    metrics.recordSort(result.products().size(), elapsed, weights);
    return result;
  }
}
