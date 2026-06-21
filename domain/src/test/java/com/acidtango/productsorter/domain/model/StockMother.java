package com.acidtango.productsorter.domain.model;

import java.util.List;

public class StockMother {

  private StockMother() {
  }

  public static Stock from(final String raw) {
    final var entries = List.of(raw.split(",")).stream()
      .map(part -> {
        final var split = part.split(":");
        return StockBySize.of(Size.valueOf(split[0].trim()), Integer.parseInt(split[1].trim()));
      })
      .toList();
    return Stock.of(entries);
  }
}
