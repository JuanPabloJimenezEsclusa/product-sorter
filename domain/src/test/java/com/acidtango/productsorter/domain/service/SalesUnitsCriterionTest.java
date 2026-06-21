package com.acidtango.productsorter.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import com.acidtango.productsorter.domain.model.*;
import org.instancio.Instancio;
import static org.instancio.Select.field;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SalesUnitsCriterionTest {

  @ParameterizedTest
  @CsvSource({
    "650, 650, 1.0",
    "0,   650, 0.0",
    "325, 650, 0.5",
    "100, 650, 0.154"
  })
  void shouldNormalizeSalesUnits(final int sales, final int maxSales, final double expected) {
    final var product = Instancio.of(Product.class)
      .set(field(Product::salesUnits), SalesUnits.of(sales))
      .set(field(Product::productId), ProductId.of(1L))
      .set(field(Product::productName), ProductName.of("Test"))
      .set(field(Product::stock), StockMother.from("S:1,M:1,L:1"))
      .create();
    final var score = new SalesUnitsCriterion(maxSales).rawScore(product);
    assertThat(score)
      .as("Sales units score should match expected")
      .isEqualTo(expected, offset(0.001));
  }
}
