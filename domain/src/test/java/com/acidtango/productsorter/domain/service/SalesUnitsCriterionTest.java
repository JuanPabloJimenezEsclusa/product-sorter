package com.acidtango.productsorter.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import com.acidtango.productsorter.domain.model.ScoreableProduct;
import com.acidtango.productsorter.domain.vo.ProductId;
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
    final var product = new ScoreableProduct(ProductId.of("1"), sales, 1.0);
    final var score = new SalesUnitsCriterion(maxSales).rawScore(product);
    assertThat(score)
      .as("Sales units score should match expected")
      .isEqualTo(expected, offset(0.001));
  }
}
