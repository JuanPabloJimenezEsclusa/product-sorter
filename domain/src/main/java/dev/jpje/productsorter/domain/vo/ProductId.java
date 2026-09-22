package dev.jpje.productsorter.domain.vo;

import java.util.Objects;

public record ProductId(String value) {
  public ProductId {
    Objects.requireNonNull(value, "ProductId must not be null");
  }

  public static ProductId of(final String value) {
    return new ProductId(value);
  }
}
