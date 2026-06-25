package dev.jpje.productsorter.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StockTest {

  @ParameterizedTest
  @CsvSource({
    "'S:1,M:1,L:1', 1.0",
    "'S:0,M:0,L:0', 0.0",
    "'S:4,M:9,L:0', 0.6667",
    "'S:0,M:1,L:0', 0.3333"
  })
  void shouldCalculateStockRatio(final String raw, final double expected) {
    final var stock = StockMother.from(raw);
    assertThat(stock.stockRatio())
      .as("Stock ratio should match expected")
      .isEqualTo(expected, offset(0.001));
  }

  @ParameterizedTest
  @CsvSource({
    "'S:1,M:1,L:1', 3",
    "'S:0,M:0,L:0', 0",
    "'S:4,M:0,L:0', 1"
  })
  void shouldCountSizesWithStock(final String raw, final long expected) {
    final var stock = StockMother.from(raw);
    assertThat(stock.sizesWithStock())
      .as("Sizes with stock should match expected")
      .isEqualTo(expected);
  }
}
