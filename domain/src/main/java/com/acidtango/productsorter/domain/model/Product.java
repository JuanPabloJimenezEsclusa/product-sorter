package com.acidtango.productsorter.domain.model;

import java.util.Objects;

import com.acidtango.productsorter.domain.vo.*;

public record Product(ProductId productId, ProductName productName, SalesUnits salesUnits, Stock stock) {
  public Product {
    Objects.requireNonNull(productId, "ProductId must not be null");
    Objects.requireNonNull(productName, "ProductName must not be null");
    Objects.requireNonNull(salesUnits, "SalesUnits must not be null");
    Objects.requireNonNull(stock, "Stock must not be null");
  }
}
