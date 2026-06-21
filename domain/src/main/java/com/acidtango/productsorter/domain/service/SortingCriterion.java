package com.acidtango.productsorter.domain.service;

import com.acidtango.productsorter.domain.model.Product;

@FunctionalInterface
public interface SortingCriterion {
  double rawScore(Product product);

  default String name() {
    return getClass().getSimpleName();
  }
}
