package dev.jpje.productsorter.domain.model;

import java.util.Map;

import dev.jpje.productsorter.domain.vo.SalesUnits;
import dev.jpje.productsorter.domain.vo.Stock;

public record AppliedWeights(double salesUnitsWeight, double stockWeight) {
  public AppliedWeights {
    if (salesUnitsWeight < 0 || stockWeight < 0) {
      throw new IllegalArgumentException("Weights must be non-negative");
    }
  }

  public static AppliedWeights fromMap(final Map<String, Double> weights) {
    Metrics.validateWeights(weights);
    return new AppliedWeights(
      weights.getOrDefault(Metrics.SALES_UNITS.key(), 0.0),
      weights.getOrDefault(Metrics.STOCK.key(), 0.0)
    );
  }

  public double computeScore(final SalesUnits salesUnits, final Stock stock, final double midpoint) {
    final var salesRatio = salesUnits.value() / (salesUnits.value() + midpoint);
    final var stockRatio = stock.stockRatio();
    return salesUnitsWeight * salesRatio + stockWeight * stockRatio;
  }
}
