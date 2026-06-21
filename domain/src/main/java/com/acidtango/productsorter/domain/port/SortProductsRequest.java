package com.acidtango.productsorter.domain.port;

import java.util.Map;

public record SortProductsRequest(Map<String, Double> weights) {
  public SortProductsRequest {
    if (weights == null || weights.isEmpty()) {
      throw new IllegalArgumentException("Weights must not be null or empty");
    }
    if (weights.values().stream().anyMatch(w -> w == null || w < 0 || w > 1)) {
      throw new IllegalArgumentException("Each weight must be between 0 and 1");
    }
  }
}
