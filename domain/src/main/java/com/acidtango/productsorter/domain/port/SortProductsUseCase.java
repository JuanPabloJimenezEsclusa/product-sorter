package com.acidtango.productsorter.domain.port;

import java.util.Map;

public interface SortProductsUseCase {
  ScoredProductPage execute(Map<String, Double> weights, int page, int size);
}
