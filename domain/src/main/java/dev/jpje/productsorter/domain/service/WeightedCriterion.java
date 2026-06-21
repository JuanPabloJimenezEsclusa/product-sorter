package dev.jpje.productsorter.domain.service;

import dev.jpje.productsorter.domain.model.ScoreableProduct;

public record WeightedCriterion(SortingCriterion wrapped, double weight) implements SortingCriterion {

  @Override
  public double rawScore(final ScoreableProduct product) {
    return weight * wrapped.rawScore(product);
  }

  @Override
  public String name() {
    return "weighted_" + wrapped.name();
  }
}
