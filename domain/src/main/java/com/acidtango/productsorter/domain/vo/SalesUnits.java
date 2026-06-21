package com.acidtango.productsorter.domain.vo;

import java.io.Serializable;

public record SalesUnits(int value) implements Serializable {
  public SalesUnits {
    if (value < 0) {
      throw new IllegalArgumentException("SalesUnits must not be negative");
    }
  }

  public static SalesUnits of(final int value) {
    return new SalesUnits(value);
  }
}
