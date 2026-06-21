package com.acidtango.productsorter.domain.vo;

import java.io.Serializable;
import java.util.Objects;

public record ProductId(String value) implements Serializable {
  public ProductId {
    Objects.requireNonNull(value, "ProductId must not be null");
  }

  public static ProductId of(final String value) {
    return new ProductId(value);
  }
}
