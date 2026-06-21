package com.acidtango.productsorter.domain.service;

import com.acidtango.productsorter.domain.model.Product;

public class StockRatioCriterion implements SortingCriterion {
  @Override
  public double rawScore(Product product) {
    return product.stock().ratio();
  }
}
