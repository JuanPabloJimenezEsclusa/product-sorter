package com.acidtango.productsorter.domain.service;

import com.acidtango.productsorter.domain.model.Product;

public record ScoredProduct(Product product, double score) {
}
