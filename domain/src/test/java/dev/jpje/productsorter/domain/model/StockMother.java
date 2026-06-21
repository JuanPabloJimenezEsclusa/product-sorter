package dev.jpje.productsorter.domain.model;

import java.util.stream.Stream;

import dev.jpje.productsorter.domain.vo.Size;
import dev.jpje.productsorter.domain.vo.Stock;
import dev.jpje.productsorter.domain.vo.StockBySize;

public class StockMother {

  private StockMother() {
  }

  public static Stock from(final String raw) {
    final var entries = Stream.of(raw.split(","))
      .map(part -> {
        final var split = part.split(":");
        return StockBySize.of(Size.valueOf(split[0].trim()), Integer.parseInt(split[1].trim()));
      })
      .toList();
    return Stock.of(entries);
  }
}
