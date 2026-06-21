package com.acidtango.productsorter.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import com.acidtango.productsorter.domain.model.ScoreableProduct;
import com.acidtango.productsorter.domain.vo.ProductId;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StockRatioCriterionTest {

  @ParameterizedTest
  @CsvSource({
    "1.0",
    "0.3333",
    "0.0",
    "0.6667"
  })
  void shouldCalculateStockScore(final double stockRatio) {
    final var product = new ScoreableProduct(ProductId.of("1"), 100, stockRatio);
    final var score = StockRatioCriterion.INSTANCE.rawScore(product);
    assertThat(score)
      .as("Stock ratio score should match expected")
      .isEqualTo(stockRatio, offset(0.001));
  }
}
