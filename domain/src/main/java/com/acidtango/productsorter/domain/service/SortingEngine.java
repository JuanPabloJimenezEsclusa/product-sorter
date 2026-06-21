package com.acidtango.productsorter.domain.service;

import com.acidtango.productsorter.domain.model.Product;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class SortingEngine {
  private final ProductScorer scorer = new ProductScorer();

  public List<ScoredProduct> sort(final List<Product> products, final Map<String, Double> weights) {
    if (products.isEmpty()) {
      return List.of();
    }
    if (weights == null || weights.isEmpty()) {
      throw new IllegalArgumentException("Weights must not be null or empty");
    }

    final var maxSalesUnits = products.stream()
      .mapToInt(p -> p.salesUnits().value())
      .max()
      .orElse(1);

    final var criteria = buildCriteria(weights, maxSalesUnits);

    return products.stream()
      .map(product -> new ScoredProduct(product, scorer.compute(criteria, product)))
      .sorted(Comparator.comparingDouble(ScoredProduct::score).reversed())
      .toList();
  }

  private List<SortingCriterion> buildCriteria(final Map<String, Double> weights, final int maxSalesUnits) {
    final var criteria = new ArrayList<SortingCriterion>();

    if (weights.containsKey("salesUnits")) {
      final var criterion = new SalesUnitsCriterion(maxSalesUnits);
      criteria.add(new WeightedCriterion(criterion, weights.get("salesUnits")));
    }
    if (weights.containsKey("stockRatio")) {
      final var criterion = new StockRatioCriterion();
      criteria.add(new WeightedCriterion(criterion, weights.get("stockRatio")));
    }

    return criteria;
  }

}

