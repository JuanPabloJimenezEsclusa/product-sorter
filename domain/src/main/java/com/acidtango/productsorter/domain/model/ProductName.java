package com.acidtango.productsorter.domain.model;

public record ProductName(String value) {
  public ProductName {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("ProductName must not be blank");
    }
  }

  public static ProductName of(String value) {
    return new ProductName(value);
  }
}
