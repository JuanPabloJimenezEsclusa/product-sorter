package com.acidtango.productsorter.infrastructure.observability;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.acidtango.productsorter.domain.port.SortProductsUseCase;
import com.acidtango.productsorter.domain.service.ScoredProduct;

public class MetricsSortProductsUseCase implements SortProductsUseCase {

  private final SortProductsUseCase delegate;
  private final SortingMetrics metrics;

  public MetricsSortProductsUseCase(final SortProductsUseCase delegate, final SortingMetrics metrics) {
    this.delegate = delegate;
    this.metrics = metrics;
  }

  @Override
  public List<ScoredProduct> execute(final Map<String, Double> weights) {
    final var start = System.nanoTime();
    final var result = delegate.execute(weights);
    final var elapsed = Duration.ofNanos(System.nanoTime() - start);
    metrics.recordSort(result.size(), elapsed, weights);
    return result;
  }
}
