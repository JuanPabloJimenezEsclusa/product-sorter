package com.acidtango.productsorter.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.acidtango.productsorter.domain.vo.*;
import org.instancio.Instancio;
import static org.instancio.Select.field;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class ProductTest {

  @ParameterizedTest(name = "{0}")
  @MethodSource("validProductScenarios")
  void shouldCreateProductWithValidAttributes(final String name, final int salesUnits, final String stockRaw) {
    final var product = Instancio.of(Product.class)
      .set(field(Product::productId), ProductId.of(1))
      .set(field(Product::productName), ProductName.of(name))
      .set(field(Product::salesUnits), SalesUnits.of(salesUnits))
      .set(field(Product::stock), StockMother.from(stockRaw))
      .create();
    assertThat(product.productName().value())
      .as("Product name should match")
      .isEqualTo(name);
    assertThat(product.salesUnits().value())
      .as("Sales units should match")
      .isEqualTo(salesUnits);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidProductScenarios")
  void shouldRejectInvalidProduct(final ThrowingCallable creation, final Class<? extends Throwable> expected) {
    assertThatThrownBy(creation)
      .as("Should reject invalid product")
      .isInstanceOf(expected);
  }

  private static Stream<Arguments> validProductScenarios() {
    return Stream.of(
      arguments(named("basic shirt", "V-NECK BASIC SHIRT"), 100, "S:4,M:9,L:0"),
      arguments(named("lace shirt", "CONTRASTING LACE T-SHIRT"), 650, "S:0,M:1,L:0"));
  }

  private static Stream<Arguments> invalidProductScenarios() {
    final var stock = StockMother.from("S:1,M:1,L:1");
    return Stream.of(
      arguments(named("null id", (ThrowingCallable) () -> new Product(null, ProductName.of("V"), SalesUnits.of(1), stock)), NullPointerException.class),
      arguments(named("blank name", (ThrowingCallable) () -> new Product(ProductId.of(1), ProductName.of(""), SalesUnits.of(1), stock)), IllegalArgumentException.class),
      arguments(named("negative sales", (ThrowingCallable) () -> new Product(ProductId.of(1), ProductName.of("V"), SalesUnits.of(-1), stock)), IllegalArgumentException.class));
  }
}
