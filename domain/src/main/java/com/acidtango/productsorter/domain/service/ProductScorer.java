package com.acidtango.productsorter.domain.service;

import com.acidtango.productsorter.domain.model.Product;

import java.util.List;

public class ProductScorer {

  public double compute(final List<SortingCriterion> criteria, final Product product) {
    return criteria.stream()
      .mapToDouble(c -> c.rawScore(product))
      .sum();
  }
}
