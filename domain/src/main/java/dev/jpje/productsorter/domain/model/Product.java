package dev.jpje.productsorter.domain.model;

import java.io.Serializable;
import java.util.Objects;

import dev.jpje.productsorter.domain.vo.*;

public record Product(ProductId productId, ProductName productName, SalesUnits salesUnits, Stock stock) implements Serializable {
  public Product {
    Objects.requireNonNull(productId, "ProductId must not be null");
    Objects.requireNonNull(productName, "ProductName must not be null");
    Objects.requireNonNull(salesUnits, "SalesUnits must not be null");
    Objects.requireNonNull(stock, "Stock must not be null");
  }
}
