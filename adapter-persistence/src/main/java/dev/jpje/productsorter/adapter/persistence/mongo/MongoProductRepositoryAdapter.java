package dev.jpje.productsorter.adapter.persistence.mongo;

import java.util.List;

import dev.jpje.productsorter.adapter.persistence.mongo.entity.ProductDocument;
import dev.jpje.productsorter.domain.model.AppliedWeights;
import dev.jpje.productsorter.domain.model.Product;
import dev.jpje.productsorter.domain.port.ProductRepository;
import dev.jpje.productsorter.domain.vo.CursorCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MongoProductRepositoryAdapter implements ProductRepository {

  private static final String COLLECTION_NAME = "products";
  private final MongoTemplate mongoTemplate;
  private final double midpoint;

  public MongoProductRepositoryAdapter(final MongoTemplate mongoTemplate,
                                        @Value("${sales.midpoint:50}") final double midpoint) {
    this.mongoTemplate = mongoTemplate;
    this.midpoint = midpoint;
  }

  @Override
  @Cacheable(
    cacheNames = "productCache",
    key = "'page-' + #limit",
    condition = "#encodedCursor == null")
  public PagedResult findPage(final String encodedCursor, final int limit) {
    final var query = MongoQueryHelper.buildPageQuery(encodedCursor, limit);
    final var products = mongoTemplate.find(query, ProductDocument.class, COLLECTION_NAME).stream()
      .map(ProductDocumentMapper::toDomain)
      .toList();

    return pageCursor(products);
  }

  @Override
  @Cacheable(
    cacheNames = "productCache",
    key = "#weights.salesUnitsWeight() + '-' + #weights.stockWeight() + '-' + #limit",
    condition = "#encodedCursor == null")
  public PagedResult sortByWeights(final AppliedWeights weights, final String encodedCursor, final int limit) {
    final var aggregation = MongoQueryHelper.buildSortAggregation(weights, encodedCursor, limit, midpoint);
    final var products = mongoTemplate.aggregate(aggregation, COLLECTION_NAME, ProductDocument.class)
      .getMappedResults().stream()
      .map(ProductDocumentMapper::toDomain)
      .toList();

    return pageCursor(products);
  }

  private static PagedResult pageCursor(final List<Product> products) {
    String nextCursor = null;
    if (!products.isEmpty()) {
      final var lastProduct = products.getLast();
      final var score = lastProduct.weightedScore() == null ? 0 : lastProduct.weightedScore();
      nextCursor = CursorCodec.encode(score, lastProduct.productId().value());
    }
    return new PagedResult(products, nextCursor);
  }
}
