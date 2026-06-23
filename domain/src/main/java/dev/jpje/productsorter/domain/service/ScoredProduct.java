package dev.jpje.productsorter.domain.service;

import dev.jpje.productsorter.domain.model.Product;

public record ScoredProduct(Product product, double score) {
}
