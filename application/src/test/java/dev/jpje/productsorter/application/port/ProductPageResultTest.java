package dev.jpje.productsorter.application.port;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import dev.jpje.productsorter.domain.model.Product;
import dev.jpje.productsorter.domain.vo.ProductId;
import dev.jpje.productsorter.domain.vo.ProductName;
import dev.jpje.productsorter.domain.vo.SalesUnits;
import dev.jpje.productsorter.domain.vo.Stock;
import org.junit.jupiter.api.Test;

class ProductPageResultTest {

  private static final Product SAMPLE = new Product(
    ProductId.of("1"), ProductName.of("Test"), SalesUnits.of(100), Stock.of(List.of()));

  @Test
  void shouldOwnCursorAndEffectiveSize() {
    final var result = new ProductPageResult(List.of(SAMPLE), "next", 10);

    assertThat(result.products()).as("products preserved").containsExactly(SAMPLE);
    assertThat(result.nextCursor()).as("cursor owned by the result").isEqualTo("next");
    assertThat(result.size()).as("effective size preserved").isEqualTo(10);
    assertThat(result.hasMore()).as("more pages when a cursor is present").isTrue();
  }

  @Test
  void shouldReportNoMoreWithoutCursor() {
    final var result = new ProductPageResult(List.of(SAMPLE), null, 10);

    assertThat(result.hasMore()).as("no more pages without a cursor").isFalse();
  }

  @Test
  void shouldCopyProductsDefensively() {
    final var mutable = new ArrayList<>(List.of(SAMPLE));
    final var result = new ProductPageResult(mutable, null, 5);

    mutable.clear();

    assertThat(result.products()).as("caller mutation does not leak into the result").hasSize(1);
  }
}
