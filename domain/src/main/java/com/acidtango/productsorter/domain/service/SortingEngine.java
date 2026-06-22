package com.acidtango.productsorter.domain.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.acidtango.productsorter.domain.model.ScoreableProduct;

public class SortingEngine {

  private final ProductScorer scorer = new ProductScorer();

  public record ScoredScoreable(ScoreableProduct product, double score) {
  }

  public List<ScoredScoreable> sortScoreables(final List<ScoreableProduct> products,
                                               final Map<String, Double> weights,
                                               final int maxSalesUnits) {
    if (products.isEmpty()) {
      return List.of();
    }
    if (weights == null || weights.isEmpty()) {
      throw new IllegalArgumentException("Weights must not be null or empty");
    }

    final var criteria = buildCriteria(weights, maxSalesUnits);

    return products.stream()
      .map(product -> new ScoredScoreable(product, scorer.compute(criteria, product)))
      .sorted(Comparator.comparingDouble(ScoredScoreable::score).reversed())
      .toList();
  }

  private List<SortingCriterion> buildCriteria(final Map<String, Double> weights, final int maxSalesUnits) {
    final var criteria = new ArrayList<SortingCriterion>();

    if (weights.containsKey("salesUnits")) {
      final var criterion = new SalesUnitsCriterion(maxSalesUnits);
      criteria.add(new WeightedCriterion(criterion, weights.get("salesUnits")));
    }
    if (weights.containsKey("stockRatio")) {
      final var criterion = StockRatioCriterion.INSTANCE;
      criteria.add(new WeightedCriterion(criterion, weights.get("stockRatio")));
    }

    return criteria;
  }
}
