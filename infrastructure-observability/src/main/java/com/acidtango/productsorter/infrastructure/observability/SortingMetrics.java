package com.acidtango.productsorter.infrastructure.observability;

import java.time.Duration;
import java.util.Map;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class SortingMetrics {

  private final Counter requests;
  private final Timer duration;
  private final DistributionSummary productsSummary;
  private final DistributionSummary salesWeight;
  private final DistributionSummary stockWeight;

  public SortingMetrics(final MeterRegistry registry) {
    this.requests = Counter.builder("sorting.requests")
      .description("Total number of sort requests")
      .register(registry);

    this.duration = Timer.builder("sorting.duration")
      .description("Time taken to sort products")
      .publishPercentiles(0.5, 0.95, 0.99)
      .register(registry);

    this.productsSummary = DistributionSummary.builder("sorting.products")
      .description("Number of products sorted per request")
      .publishPercentiles(0.5, 0.95, 0.99)
      .register(registry);

    this.salesWeight = DistributionSummary.builder("sorting.weights")
      .tag("criterion", "salesUnits")
      .description("Sales units weight used in requests")
      .register(registry);

    this.stockWeight = DistributionSummary.builder("sorting.weights")
      .tag("criterion", "stockRatio")
      .description("Stock ratio weight used in requests")
      .register(registry);
  }

  public void recordSort(final int productCount, final Duration elapsed, final Map<String, Double> weights) {
    requests.increment();
    duration.record(elapsed);
    productsSummary.record(productCount);
    if (weights.containsKey("salesUnits")) {
      salesWeight.record(weights.get("salesUnits"));
    }
    if (weights.containsKey("stockRatio")) {
      stockWeight.record(weights.get("stockRatio"));
    }
  }
}
