package com.acidtango.productsorter.domain.model;

public record StockBySize(Size size, int quantity) {
  public StockBySize {
    if (quantity < 0) {
      throw new IllegalArgumentException("Stock quantity must not be negative");
    }
  }

  public static StockBySize of(Size size, int quantity) {
    return new StockBySize(size, quantity);
  }

  public boolean hasStock() {
    return quantity > 0;
  }
}
