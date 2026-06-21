package dev.jpje.productsorter.domain.model;

import java.io.Serializable;

import dev.jpje.productsorter.domain.vo.ProductId;

public record ScoreableProduct(ProductId productId, int salesUnits, double stockRatio) implements Serializable {
}
