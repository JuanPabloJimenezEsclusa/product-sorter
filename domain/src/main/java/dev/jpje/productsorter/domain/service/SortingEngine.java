package dev.jpje.productsorter.domain.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import dev.jpje.productsorter.domain.model.ScoreableProduct;
import dev.jpje.productsorter.domain.vo.CriterionType;
import dev.jpje.productsorter.domain.vo.ProductId;

public class SortingEngine {

  private final ProductScorer scorer = new ProductScorer();

  public record ScoredEntry(ProductId productId, double score) {
  }

  public List<ScoredEntry> sortScoreables(final List<ScoreableProduct> products,
                                          final Map<String, Double> weights,
                                          final int maxSalesUnits) {
    if (products.isEmpty()) {
      return List.of();
    }

    final var criteria = buildCriteria(weights, maxSalesUnits);

    return products.stream()
      .map(product -> new ScoredEntry(product.productId(), scorer.compute(criteria, product)))
      .sorted(Comparator.comparingDouble(ScoredEntry::score).reversed())
      .toList();
  }

  private List<SortingCriterion> buildCriteria(final Map<String, Double> weights, final int maxSalesUnits) {
    final var criteria = new ArrayList<SortingCriterion>();

    if (weights.containsKey(CriterionType.SALES_UNITS.key())) {
      final var criterion = new SalesUnitsCriterion(maxSalesUnits);
      criteria.add(new WeightedCriterion(criterion, weights.get(CriterionType.SALES_UNITS.key())));
    }
    if (weights.containsKey(CriterionType.STOCK_RATIO.key())) {
      final var criterion = StockRatioCriterion.INSTANCE;
      criteria.add(new WeightedCriterion(criterion, weights.get(CriterionType.STOCK_RATIO.key())));
    }

    return criteria;
  }
}
