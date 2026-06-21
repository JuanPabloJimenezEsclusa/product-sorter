package com.acidtango.productsorter.domain.model;

public record ProductId(Long value) {
  public ProductId {
    if (value == null) {
      throw new IllegalArgumentException("ProductId must not be null");
    }
  }

  public static ProductId of(long value) {
    return new ProductId(value);
  }
}
