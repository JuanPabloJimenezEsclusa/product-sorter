package dev.jpje.productsorter.adapter.persistence.mongo;

import java.util.ArrayList;
import java.util.List;

import dev.jpje.productsorter.domain.model.AppliedWeights;
import dev.jpje.productsorter.domain.vo.CursorCodec;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.AddFieldsOperation;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationExpression;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;

final class ProductSorterHelper {

  private static final String WEIGHTED_SCORE = "weightedScore";
  private static final String SALES_UNITS = "$salesUnits";
  private static final String STOCK = "$stock";
  private static final String ID = "_id";

  private ProductSorterHelper() {
  }

  static Aggregation buildAggregation(final AppliedWeights appliedWeights, final String encodedCursor, final int limit) {
    final List<AggregationOperation> stages = new ArrayList<>();

    stages.add(buildWeightedScoreField(appliedWeights));

    final var cursor = encodedCursor != null ? CursorCodec.decode(encodedCursor) : null;

    if (cursor != null) {
      stages.add(buildSortOperation());
      stages.add(buildCursorMatch(cursor));
    }

    stages.add(buildSortOperation());
    stages.add(Aggregation.limit(limit));

    return Aggregation.newAggregation(stages)
      .withOptions(AggregationOptions.builder().allowDiskUse(true).build());
  }

  private static AddFieldsOperation buildWeightedScoreField(final AppliedWeights appliedWeights) {
    final var expressions = new ArrayList<AggregationExpression>();

    if (appliedWeights.salesUnitsWeight() > 0) {
      expressions.add(ArithmeticOperators.Multiply.valueOf(SALES_UNITS)
        .multiplyBy(appliedWeights.salesUnitsWeight()));
    }

    if (appliedWeights.stockWeight() > 0) {
      final var condition = ComparisonOperators.valueOf("$$s.quantity").greaterThanValue(0);
      final var sizesWithStock = ArrayOperators.Filter.filter(STOCK).as("s").by(condition);
      final var stockRatio = ConditionalOperators.Cond
        .when(ComparisonOperators.valueOf(ArrayOperators.Size.lengthOfArray(STOCK))
          .greaterThanValue(0))
        .then(ArithmeticOperators.Divide.valueOf(
          ArrayOperators.Size.lengthOfArray(sizesWithStock)
        ).divideBy(
          ArrayOperators.Size.lengthOfArray(STOCK)
        ))
        .otherwise(0);
      expressions.add(ArithmeticOperators.Multiply.valueOf(stockRatio)
        .multiplyBy(appliedWeights.stockWeight()));
    }

    final var weightedScore = expressions.stream()
      .reduce((a, b) -> ArithmeticOperators.Add.valueOf(a).add(b))
      .orElse(ArithmeticOperators.Multiply.valueOf(SALES_UNITS).multiplyBy(1.0));

    return Aggregation.addFields()
      .addFieldWithValue(WEIGHTED_SCORE, weightedScore)
      .build();
  }

  private static SortOperation buildSortOperation() {
    return Aggregation.sort(Sort.Direction.DESC, WEIGHTED_SCORE, ID);
  }

  private static MatchOperation buildCursorMatch(final CursorCodec.DecodedCursor cursor) {
    final var criteria = new Criteria().orOperator(
      Criteria.where(WEIGHTED_SCORE).lt(cursor.score()),
      Criteria.where(WEIGHTED_SCORE).is(cursor.score()).and(ID).lt(cursor.productId())
    );
    return Aggregation.match(criteria);
  }
}
