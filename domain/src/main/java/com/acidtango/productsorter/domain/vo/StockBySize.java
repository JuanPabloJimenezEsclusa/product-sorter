package com.acidtango.productsorter.domain.vo;

import java.io.Serializable;

public record StockBySize(Size size, int quantity) implements Serializable {
  public StockBySize {
    if (quantity < 0) {
      throw new IllegalArgumentException("Stock quantity must not be negative");
    }
  }

  public static StockBySize of(final Size size, final int quantity) {
    return new StockBySize(size, quantity);
  }

  public boolean hasStock() {
    return quantity > 0;
  }
}
