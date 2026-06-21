package com.acidtango.productsorter.domain.vo;

import java.util.Objects;

public record ProductName(String value) {
  public ProductName {
    Objects.requireNonNull(value, "ProductName must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException("ProductName must not be blank");
    }
  }

  public static ProductName of(final String value) {
    return new ProductName(value);
  }
}
