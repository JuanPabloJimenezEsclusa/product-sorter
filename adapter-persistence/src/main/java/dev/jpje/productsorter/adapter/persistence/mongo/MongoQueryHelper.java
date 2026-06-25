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
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

final class MongoQueryHelper {

  private static final String WEIGHTED_SCORE = "weightedScore";
  private static final String SALES_UNITS = "$salesUnits";
  private static final String STOCK_RATIO = "$stockRatio";
  private static final String ID = "_id";

  private MongoQueryHelper() {
  }

  static Query buildPageQuery(final String encodedCursor, final int limit) {
    final var query = new Query()
      .with(Sort.by(Sort.Direction.ASC, ID))
      .limit(limit);
    if (encodedCursor != null) {
      query.addCriteria(Criteria.where(ID).gt(CursorCodec.decode(encodedCursor).productId()));
    }
    return query;
  }

  static Aggregation buildSortAggregation(final AppliedWeights appliedWeights, final String encodedCursor,
                                          final int limit, final double midpoint) {
    final List<AggregationOperation> stages = new ArrayList<>();

    stages.add(buildWeightedScoreField(appliedWeights, midpoint));

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

  private static AddFieldsOperation buildWeightedScoreField(final AppliedWeights appliedWeights, final double midpoint) {
    final var expressions = new ArrayList<AggregationExpression>();

    if (appliedWeights.salesUnitsWeight() > 0) {
      final var salesRatio = ArithmeticOperators.Divide.valueOf(SALES_UNITS)
        .divideBy(ArithmeticOperators.Add.valueOf(SALES_UNITS).add(midpoint));
      expressions.add(ArithmeticOperators.Multiply.valueOf(salesRatio)
        .multiplyBy(appliedWeights.salesUnitsWeight()));
    }

    if (appliedWeights.stockWeight() > 0) {
      expressions.add(ArithmeticOperators.Multiply.valueOf(STOCK_RATIO)
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
