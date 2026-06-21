package com.acidtango.productsorter.domain.port;

import java.util.List;
import java.util.Map;

import com.acidtango.productsorter.domain.service.ScoredProduct;

public interface SortProductsUseCase {
  List<ScoredProduct> execute(Map<String, Double> weights, int page, int size);
}
