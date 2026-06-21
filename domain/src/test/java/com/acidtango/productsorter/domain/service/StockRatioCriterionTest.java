package com.acidtango.productsorter.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.instancio.Select.field;

import com.acidtango.productsorter.domain.model.Product;
import com.acidtango.productsorter.domain.model.StockMother;
import com.acidtango.productsorter.domain.vo.ProductId;
import com.acidtango.productsorter.domain.vo.ProductName;
import com.acidtango.productsorter.domain.vo.SalesUnits;
import org.instancio.Instancio;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StockRatioCriterionTest {

  @ParameterizedTest
  @CsvSource({
    "'S:1,M:1,L:1', 1.0",
    "'S:0,M:0,L:0', 0.0",
    "'S:4,M:9,L:0', 0.6667",
    "'S:0,M:1,L:0', 0.3333"
  })
  void shouldCalculateStockScore(final String stockRaw, final double expected) {
    final var product = Instancio.of(Product.class)
      .set(field(Product::stock), StockMother.from(stockRaw))
      .set(field(Product::productId), ProductId.of(1L))
      .set(field(Product::productName), ProductName.of("Test"))
      .set(field(Product::salesUnits), SalesUnits.of(100))
      .create();
    final var score = new StockRatioCriterion().rawScore(product);
    assertThat(score)
      .as("Stock ratio score should match expected")
      .isEqualTo(expected, offset(0.001));
  }
}
