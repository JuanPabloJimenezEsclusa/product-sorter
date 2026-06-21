package com.acidtango.productsorter.domain.model;

public record Product(ProductId productId, ProductName productName, SalesUnits salesUnits, Stock stock) {
  public Product {
    if (productId == null) {
      throw new IllegalArgumentException("ProductId must not be null");
    }
    if (productName == null) {
      throw new IllegalArgumentException("ProductName must not be null");
    }
    if (salesUnits == null) {
      throw new IllegalArgumentException("SalesUnits must not be null");
    }
    if (stock == null) {
      throw new IllegalArgumentException("Stock must not be null");
    }
  }
}
