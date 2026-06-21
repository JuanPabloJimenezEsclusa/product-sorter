package com.acidtango.productsorter.domain.port;

import java.util.Map;

import com.acidtango.productsorter.domain.service.ScoredProductPage;

public interface SortProductsUseCase {
  ScoredProductPage execute(Map<String, Double> weights, int page, int size);
}
