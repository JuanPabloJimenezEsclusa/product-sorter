package com.acidtango.productsorter.domain.service;

import com.acidtango.productsorter.domain.model.ScoreableProduct;

public enum StockRatioCriterion implements SortingCriterion {
  INSTANCE;

  @Override
  public double rawScore(final ScoreableProduct product) {
    return product.stockRatio();
  }
}
