package com.acidtango.productsorter.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import java.util.List;

class ProductTest {

  @Test
  void shouldCreateProductWithValidAttributes() {
    final var product = new Product(
      ProductId.of(1),
      ProductName.of("V-NECK BASIC SHIRT"),
      SalesUnits.of(100),
      StockMother.from("S:4,M:9,L:0"));
    assertThat(product.productName().value())
      .as("Product name should match")
      .isEqualTo("V-NECK BASIC SHIRT");
    assertThat(product.salesUnits().value())
      .as("Sales units should match")
      .isEqualTo(100);
  }

  @Test
  void shouldRejectNullProductId() {
    final var stock = StockMother.from("S:1,M:1,L:1");
    assertThatThrownBy(() -> new Product(null, ProductName.of("V"), SalesUnits.of(1), stock))
      .as("Null product ID should throw")
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectBlankProductName() {
    final var stock = StockMother.from("S:1,M:1,L:1");
    assertThatThrownBy(() -> new Product(ProductId.of(1), ProductName.of(""), SalesUnits.of(1), stock))
      .as("Blank product name should throw")
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNegativeSalesUnits() {
    final var stock = StockMother.from("S:1,M:1,L:1");
    assertThatThrownBy(() -> new Product(ProductId.of(1), ProductName.of("V"), SalesUnits.of(-1), stock))
      .as("Negative sales units should throw")
      .isInstanceOf(IllegalArgumentException.class);
  }
}
