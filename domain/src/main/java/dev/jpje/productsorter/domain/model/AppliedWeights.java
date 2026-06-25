package dev.jpje.productsorter.domain.model;

import java.util.Map;

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
}
