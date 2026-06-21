package dev.jpje.productsorter.domain.service;

import java.util.List;

import dev.jpje.productsorter.domain.model.ScoreableProduct;

public class ProductScorer {

  public double compute(final List<SortingCriterion> criteria, final ScoreableProduct product) {
    return criteria.stream()
      .mapToDouble(c -> c.rawScore(product))
      .sum();
  }
}
