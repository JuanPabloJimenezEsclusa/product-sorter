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
import org.springframework.data.mongodb.core.query.Query;

final class MongoQueryHelper {

  private static final String WEIGHTED_SCORE = "weightedScore";
  private static final String SALES_UNITS = "$salesUnits";
  private static final String STOCK = "$stock";
  private static final String STOCK_ENTRY = "stockEntry";
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
    final var salesRatio = ArithmeticOperators.Divide.valueOf(SALES_UNITS)
      .divideBy(ArithmeticOperators.Add.valueOf(SALES_UNITS).add(midpoint));
    final var salesTerm = ArithmeticOperators.Multiply.valueOf(salesRatio)
      .multiplyBy(appliedWeights.salesUnitsWeight());
    final var stockTerm = ArithmeticOperators.Multiply.valueOf(buildStockRatioExpression())
      .multiplyBy(appliedWeights.stockWeight());

    return Aggregation.addFields()
      .addFieldWithValue(WEIGHTED_SCORE, ArithmeticOperators.Add.valueOf(salesTerm).add(stockTerm))
      .build();
  }

  /**
   * Derives the stock ratio from the document's embedded stock entries: the fraction of entries with
   * quantity greater than zero, or {@code 0.0} when there are none. The denormalized stored
   * {@code stockRatio} field is never read, so the ranked read and the domain rule share one source of
   * truth for the stock contribution.
   */
  private static AggregationExpression buildStockRatioExpression() {
    final var totalEntries = ArrayOperators.Size.lengthOfArray(STOCK);
    final var entriesWithStock = ArrayOperators.Size.lengthOfArray(
      ArrayOperators.Filter.filter(STOCK)
        .as(STOCK_ENTRY)
        .by(ComparisonOperators.Gt.valueOf("$$" + STOCK_ENTRY + ".quantity").greaterThanValue(0)));

    return ConditionalOperators.Cond
      .when(ComparisonOperators.Eq.valueOf(totalEntries).equalToValue(0))
      .then(0.0)
      .otherwiseValueOf(ArithmeticOperators.Divide.valueOf(entriesWithStock).divideBy(totalEntries));
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
