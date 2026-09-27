package dev.jpje.productsorter.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.offset;

import java.util.ArrayList;
import java.util.List;

import dev.jpje.productsorter.domain.vo.Size;
import dev.jpje.productsorter.domain.vo.Stock;
import dev.jpje.productsorter.domain.vo.StockBySize;
import org.junit.jupiter.api.Test;
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

  @Test
  void shouldCopyEntriesSoCallerMutationDoesNotChangeTheCollection() {
    final var callerList = new ArrayList<StockBySize>();
    callerList.add(StockBySize.of(Size.of("M"), 1));
    final var stock = Stock.of(callerList);

    callerList.clear();

    assertThat(stock.entries())
      .as("Stock must not retain a mutation handle on the caller's list")
      .containsExactly(StockBySize.of(Size.of("M"), 1));
  }

  @Test
  void shouldExposeUnmodifiableEntries() {
    final var entries = Stock.of(List.of(StockBySize.of(Size.of("S"), 1))).entries();

    assertThatThrownBy(entries::clear)
      .as("Entries must be unmodifiable")
      .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void shouldRejectNullEntriesWithTheExistingMessage() {
    assertThatThrownBy(() -> Stock.of(null))
      .as("Null entry list must be rejected")
      .isInstanceOf(NullPointerException.class)
      .hasMessageContaining("Stock entries must not be null");
  }
}
