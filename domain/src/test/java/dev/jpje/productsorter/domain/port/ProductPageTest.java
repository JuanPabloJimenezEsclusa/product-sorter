package dev.jpje.productsorter.domain.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.jpje.productsorter.domain.model.Product;
import dev.jpje.productsorter.domain.vo.ProductId;
import dev.jpje.productsorter.domain.vo.ProductName;
import dev.jpje.productsorter.domain.vo.SalesUnits;
import dev.jpje.productsorter.domain.vo.Stock;
import org.junit.jupiter.api.Test;

class ProductPageTest {

  private static final Product SAMPLE = new Product(
    ProductId.of("1"), ProductName.of("Test"), SalesUnits.of(100), Stock.of(List.of()));

  @Test
  void shouldCarryOnlyRawRows() {
    final var page = new ProductPage(List.of(SAMPLE));

    assertThat(page.products()).as("products preserved").containsExactly(SAMPLE);
    assertThat(Arrays.stream(ProductPage.class.getRecordComponents())
      .map(component -> component.getName().toLowerCase()))
      .as("adapter page declares no client-facing cursor component")
      .noneMatch(name -> name.contains("cursor"));
  }

  @Test
  void shouldCopyProductsDefensively() {
    final var mutable = new ArrayList<>(List.of(SAMPLE));
    final var page = new ProductPage(mutable);

    mutable.clear();

    assertThat(page.products()).as("caller mutation does not leak into the page").hasSize(1);
  }

  @Test
  void shouldExposeUnmodifiableProducts() {
    final var page = new ProductPage(List.of(SAMPLE));

    assertThatThrownBy(() -> page.products().add(SAMPLE))
      .as("returned products list is unmodifiable")
      .isInstanceOf(UnsupportedOperationException.class);
  }
}
