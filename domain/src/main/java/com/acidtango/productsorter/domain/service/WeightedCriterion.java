package com.acidtango.productsorter.domain.service;

import com.acidtango.productsorter.domain.model.Product;

public class WeightedCriterion implements SortingCriterion {
  private final SortingCriterion wrapped;
  private final double weight;

  public WeightedCriterion(final SortingCriterion wrapped, final double weight) {
    this.wrapped = wrapped;
    this.weight = weight;
  }

  @Override
  public double rawScore(final Product product) {
    return weight * wrapped.rawScore(product);
  }

  @Override
  public String name() {
    return "weighted_" + wrapped.name();
  }

  public SortingCriterion wrapped() {
    return wrapped;
  }

  public double weight() {
    return weight;
  }
}
