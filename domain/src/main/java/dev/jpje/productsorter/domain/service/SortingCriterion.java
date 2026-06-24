package dev.jpje.productsorter.domain.service;

import dev.jpje.productsorter.domain.model.ScoreableProduct;

@FunctionalInterface
public interface SortingCriterion {
  double rawScore(ScoreableProduct product);

  default String name() {
    return getClass().getSimpleName();
  }
}
