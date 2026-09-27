package dev.jpje.productsorter.adapter.persistence.mongo;

import dev.jpje.productsorter.adapter.persistence.mongo.entity.ProductDocument;
import dev.jpje.productsorter.domain.model.AppliedWeights;
import dev.jpje.productsorter.domain.port.ProductPage;
import dev.jpje.productsorter.domain.port.ProductRepository;
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
  public ProductPage findPage(final String encodedCursor, final int limit) {
    final var query = MongoQueryHelper.buildPageQuery(encodedCursor, limit);
    final var products = mongoTemplate.find(query, ProductDocument.class, COLLECTION_NAME).stream()
      .map(ProductDocumentMapper::toDomain)
      .toList();

    return new ProductPage(products);
  }

  @Override
  @Cacheable(
    cacheNames = "productCache",
    key = "#weights.toString() + '-' + #limit",
    condition = "#encodedCursor == null")
  public ProductPage sortByWeights(final AppliedWeights weights, final String encodedCursor, final int limit) {
    final var aggregation = MongoQueryHelper.buildSortAggregation(weights, encodedCursor, limit, midpoint);
    final var products = mongoTemplate.aggregate(aggregation, COLLECTION_NAME, ProductDocument.class)
      .getMappedResults().stream()
      .map(ProductDocumentMapper::toDomain)
      .toList();

    return new ProductPage(products);
  }
}
