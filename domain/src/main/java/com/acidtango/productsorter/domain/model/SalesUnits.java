package com.acidtango.productsorter.domain.model;

public record SalesUnits(int value) {
  public SalesUnits {
    if (value < 0) {
      throw new IllegalArgumentException("SalesUnits must not be negative");
    }
  }

  public static SalesUnits of(int value) {
    return new SalesUnits(value);
  }
}
