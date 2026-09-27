package dev.jpje.productsorter.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;

import dev.jpje.productsorter.domain.model.Product;
import dev.jpje.productsorter.domain.vo.CursorCodec;
import dev.jpje.productsorter.domain.vo.ProductId;
import dev.jpje.productsorter.domain.vo.ProductName;
import dev.jpje.productsorter.domain.vo.SalesUnits;
import dev.jpje.productsorter.domain.vo.Stock;
import org.junit.jupiter.api.Test;

class PageAssemblerTest {

  @Test
  void shouldTrimOverFetchedRowAndEncodeCursorFromLastVisibleProduct() {
    final var fetched = products(3);

    final var result = PageAssembler.assemble(fetched, 2, Product::weightedScore);

    assertThat(result.products()).as("visible page trimmed to the requested size").hasSize(2);
    assertThat(result.size()).as("effective size").isEqualTo(2);
    final var decoded = CursorCodec.decode(result.nextCursor());
    assertThat(decoded.productId()).as("cursor encodes the last visible product")
      .isEqualTo(result.products().getLast().productId().value());
  }

  @Test
  void shouldNotProduceCursorWhenPageNotFull() {
    final var result = PageAssembler.assemble(products(2), 5, Product::weightedScore);

    assertThat(result.products()).as("all rows are visible").hasSize(2);
    assertThat(result.nextCursor()).as("no cursor without an over-fetched row").isNull();
    assertThat(result.hasMore()).as("no more pages").isFalse();
  }

  @Test
  void shouldUseSuppliedScoreForCursorEncoding() {
    final var result = PageAssembler.assemble(products(2), 1, product -> 0.75);

    assertThat(CursorCodec.decode(result.nextCursor()).score())
      .as("cursor uses the supplied cursor-score function").isEqualTo(0.75);
  }

  private static List<Product> products(final int count) {
    return IntStream.range(0, count)
      .mapToObj(i -> new Product(ProductId.of(String.valueOf(i + 1)), ProductName.of("P" + (i + 1)),
        SalesUnits.of((i + 1) * 10), Stock.of(List.of()), (i + 1) * 0.1))
      .toList();
  }
}
