package com.acidtango.productsorter.domain.vo;

import java.util.Objects;

public record ProductId(Long value) {
  public ProductId {
    Objects.requireNonNull(value, "ProductId must not be null");
  }

  public static ProductId of(final long value) {
    return new ProductId(value);
  }
}
