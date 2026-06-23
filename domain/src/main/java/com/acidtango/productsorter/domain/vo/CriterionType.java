package com.acidtango.productsorter.domain.vo;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum CriterionType {
  SALES_UNITS("salesUnits"),
  STOCK_RATIO("stockRatio");

  private final String key;

  CriterionType(final String key) {
    this.key = key;
  }

  public String key() {
    return key;
  }

  public static void validateWeights(final Map<String, Double> weights) {
    if (weights == null || weights.isEmpty()) {
      throw new IllegalArgumentException("Weights must not be null or empty");
    }
    final var validKeys = Stream.of(values()).map(CriterionType::key).collect(Collectors.toSet());
    for (final var key : weights.keySet()) {
      if (!validKeys.contains(key)) {
        throw new IllegalArgumentException("Unknown criterion: " + key);
      }
    }
    if (weights.values().stream().anyMatch(w -> w == null || w < 0 || w > 1)) {
      throw new IllegalArgumentException("Each weight must be between 0 and 1");
    }
  }
}
