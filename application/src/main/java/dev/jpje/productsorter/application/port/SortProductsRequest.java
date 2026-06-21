package dev.jpje.productsorter.application.port;

import java.util.Map;

import dev.jpje.productsorter.domain.vo.CriterionType;

public record SortProductsRequest(Map<String, Double> weights) {
  public SortProductsRequest {
    CriterionType.validateWeights(weights);
  }
}
