package com.acidtango.productsorter.domain.service;

import java.util.List;

import com.acidtango.productsorter.domain.model.Product;

public class ProductScorer {

  public double compute(final List<SortingCriterion> criteria, final Product product) {
    return criteria.stream()
      .mapToDouble(c -> c.rawScore(product))
      .sum();
  }
}
