package com.acidtango.productsorter.domain.model;

import com.acidtango.productsorter.domain.vo.ProductId;

public record ScoreableProduct(ProductId productId, int salesUnits, double stockRatio) {
}
