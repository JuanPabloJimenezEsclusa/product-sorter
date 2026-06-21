package com.acidtango.productsorter.domain.model;

import com.acidtango.productsorter.domain.vo.Size;
import com.acidtango.productsorter.domain.vo.Stock;
import com.acidtango.productsorter.domain.vo.StockBySize;
import java.util.stream.Stream;

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
