package com.acidtango.productsorter.domain.port;

import java.util.Map;

import com.acidtango.productsorter.domain.vo.CriterionType;

public record SortProductsRequest(Map<String, Double> weights) {
  public SortProductsRequest {
    CriterionType.validateWeights(weights);
  }
}
