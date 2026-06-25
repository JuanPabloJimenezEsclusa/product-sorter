package dev.jpje.productsorter.domain.vo;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record Stock(List<StockBySize> entries) implements Serializable {
  public Stock {
    Objects.requireNonNull(entries, "Stock entries must not be null");
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

  public double stockRatio() {
    if (entries().isEmpty()) {
      return 0.0;
    }
    return (double) sizesWithStock() / entries().size();
  }
}
