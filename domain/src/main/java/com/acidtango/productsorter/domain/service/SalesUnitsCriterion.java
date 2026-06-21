package com.acidtango.productsorter.domain.service;

import com.acidtango.productsorter.domain.model.ScoreableProduct;

public record SalesUnitsCriterion(int maxSalesUnits) implements SortingCriterion {

  public SalesUnitsCriterion {
    if (maxSalesUnits <= 0) {
      throw new IllegalArgumentException("maxSalesUnits must be positive");
    }
  }

  @Override
  public double rawScore(final ScoreableProduct product) {
    return (double) product.salesUnits() / maxSalesUnits;
  }
}
