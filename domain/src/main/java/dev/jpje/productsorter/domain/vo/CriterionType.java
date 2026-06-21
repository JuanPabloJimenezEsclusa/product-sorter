package dev.jpje.productsorter.domain.vo;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum CriterionType {
  SALES_UNITS("salesUnits"),
  STOCK_RATIO("stockRatio");

  private static final Set<String> VALID_KEYS = Stream.of(values())
    .map(CriterionType::key)
    .collect(Collectors.toSet());

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
    for (final var key : weights.keySet()) {
      if (!VALID_KEYS.contains(key)) {
        throw new IllegalArgumentException("Unknown criterion: " + key);
      }
    }
    if (weights.values().stream().anyMatch(w -> w == null || w < 0 || w > 1)) {
      throw new IllegalArgumentException("Each weight must be between 0 and 1");
    }
  }
}
