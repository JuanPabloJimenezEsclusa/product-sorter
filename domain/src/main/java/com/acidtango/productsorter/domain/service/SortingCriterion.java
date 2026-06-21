package com.acidtango.productsorter.domain.service;

import com.acidtango.productsorter.domain.model.ScoreableProduct;

@FunctionalInterface
public interface SortingCriterion {
  double rawScore(ScoreableProduct product);

  default String name() {
    return getClass().getSimpleName();
  }
}
