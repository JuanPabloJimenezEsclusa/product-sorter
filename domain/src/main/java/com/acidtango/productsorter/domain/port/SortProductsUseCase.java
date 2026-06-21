package com.acidtango.productsorter.domain.port;

import com.acidtango.productsorter.domain.service.ScoredProduct;

import java.util.List;
import java.util.Map;

public interface SortProductsUseCase {
  List<ScoredProduct> execute(Map<String, Double> weights);
}
