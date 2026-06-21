package com.acidtango.productsorter.domain.model;

import java.util.Collections;
import java.util.List;

public record Stock(List<StockBySize> entries) {
  public Stock {
    if (entries == null) {
      throw new IllegalArgumentException("Stock entries must not be null");
    }
  }

  public static Stock of(final List<StockBySize> entries) {
    return new Stock(entries);
  }

  public List<StockBySize> entries() {
    return Collections.unmodifiableList(entries);
  }

  public long sizesWithStock() {
    return entries().stream()
      .filter(StockBySize::hasStock)
      .count();
  }

  public double ratio() {
    if (entries().isEmpty()) {
      return 0.0;
    }
    return (double) sizesWithStock() / entries().size();
  }
}
