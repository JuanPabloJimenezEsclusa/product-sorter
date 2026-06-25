package dev.jpje.productsorter.domain.port;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import dev.jpje.productsorter.domain.model.Product;
import dev.jpje.productsorter.domain.vo.ProductId;
import dev.jpje.productsorter.domain.vo.ProductName;
import dev.jpje.productsorter.domain.vo.SalesUnits;
import dev.jpje.productsorter.domain.vo.Stock;
import org.junit.jupiter.api.Test;

class PagedResultTest {

  private static final Product SAMPLE = new Product(
    ProductId.of("1"), ProductName.of("Test"), SalesUnits.of(100), Stock.of(List.of()));

  @Test
  void shouldCreateWithProducts() {
    final var result = new ProductRepository.PagedResult(List.of(SAMPLE), null);
    assertThat(result.products()).as("products preserved").hasSize(1);
    assertThat(result.nextCursor()).as("no next cursor").isNull();
    assertThat(result.hasMore()).as("no more pages").isFalse();
  }

  @Test
  void shouldDetectHasMore() {
    final var result = new ProductRepository.PagedResult(List.of(SAMPLE), "nextCursor");
    assertThat(result.hasMore()).isTrue();
  }

  @Test
  void shouldCopyProducts() {
    final var mutable = new java.util.ArrayList<>(List.of(SAMPLE));
    final var result = new ProductRepository.PagedResult(mutable, null);
    mutable.clear();
    assertThat(result.products()).hasSize(1);
  }
}
