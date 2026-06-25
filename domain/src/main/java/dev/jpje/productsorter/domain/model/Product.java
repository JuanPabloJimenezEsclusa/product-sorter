package dev.jpje.productsorter.domain.model;

import java.io.Serializable;
import java.util.Objects;

import dev.jpje.productsorter.domain.vo.*;

public record Product(ProductId productId, ProductName productName, SalesUnits salesUnits, Stock stock,
                      Double weightedScore) implements Serializable {
  public Product {
    Objects.requireNonNull(productId, "ProductId must not be null");
    Objects.requireNonNull(productName, "ProductName must not be null");
    Objects.requireNonNull(salesUnits, "SalesUnits must not be null");
    Objects.requireNonNull(stock, "Stock must not be null");
  }

  public Product(ProductId productId, ProductName productName, SalesUnits salesUnits, Stock stock) {
    this(productId, productName, salesUnits, stock, null);
  }
}
