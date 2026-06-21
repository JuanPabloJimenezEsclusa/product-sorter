package com.acidtango.productsorter.domain.service;

import com.acidtango.productsorter.domain.model.Product;

public class SalesUnitsCriterion implements SortingCriterion {
  private final int maxSalesUnits;

  public SalesUnitsCriterion(final int maxSalesUnits) {
    if (maxSalesUnits <= 0) {
      throw new IllegalArgumentException("maxSalesUnits must be positive");
    }
    this.maxSalesUnits = maxSalesUnits;
  }

  @Override
  public double rawScore(final Product product) {
    return (double) product.salesUnits().value() / maxSalesUnits;
  }
}
