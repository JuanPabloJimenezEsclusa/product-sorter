package dev.jpje.productsorter.application.port;

import java.util.Map;

import dev.jpje.productsorter.domain.model.Metrics;

public record SortProductsRequest(Map<String, Double> weights) {
  public SortProductsRequest {
    Metrics.validateWeights(weights);
  }
}
