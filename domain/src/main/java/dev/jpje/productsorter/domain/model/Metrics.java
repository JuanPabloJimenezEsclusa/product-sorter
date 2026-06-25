package dev.jpje.productsorter.domain.model;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Metrics {
  SALES_UNITS("salesUnits"),
  STOCK("stockRatio");

  private static final Set<String> VALID_KEYS = Stream.of(values())
    .map(Metrics::key)
    .collect(Collectors.toSet());

  private final String key;

  Metrics(final String key) {
    this.key = key;
  }

  public String key() {
    return key;
  }

  public static Metrics fromKey(final String key) {
    return Stream.of(values())
      .filter(m -> m.key.equals(key))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Unknown metric: " + key));
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
    for (final var weight : weights.values()) {
      if (weight == null || weight < 0 || weight > 1) {
        throw new IllegalArgumentException("Each weight must be between 0 and 1");
      }
    }
  }
}
