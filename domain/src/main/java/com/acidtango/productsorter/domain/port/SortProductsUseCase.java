package com.acidtango.productsorter.domain.port;

import com.acidtango.productsorter.domain.model.ProductPage;
import com.acidtango.productsorter.domain.service.ScoredProductPage;
import java.util.Map;

public interface SortProductsUseCase {
  ScoredProductPage execute(Map<String, Double> weights, int page, int size);
}
