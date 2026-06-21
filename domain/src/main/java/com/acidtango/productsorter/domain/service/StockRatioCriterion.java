package com.acidtango.productsorter.domain.service;

import com.acidtango.productsorter.domain.model.ScoreableProduct;

@SuppressWarnings("java:S6548") // stateless strategy — enum singleton is the correct pattern
public enum StockRatioCriterion implements SortingCriterion {
  INSTANCE;

  @Override
  public double rawScore(final ScoreableProduct product) {
    return product.stockRatio();
  }
}
