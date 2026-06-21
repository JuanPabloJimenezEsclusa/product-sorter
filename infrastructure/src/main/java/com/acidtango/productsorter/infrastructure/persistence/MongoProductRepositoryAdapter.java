package com.acidtango.productsorter.infrastructure.persistence;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.List;
import java.util.OptionalInt;

import com.acidtango.productsorter.domain.model.Product;
import com.acidtango.productsorter.domain.model.ScoreableProduct;
import com.acidtango.productsorter.domain.port.ProductRepository;
import com.acidtango.productsorter.domain.vo.ProductId;
import com.acidtango.productsorter.infrastructure.persistence.entity.ProductDocument;
import org.bson.Document;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class MongoProductRepositoryAdapter implements ProductRepository {

  private final MongoTemplate mongoTemplate;
  private final ProductDocumentMapper mapper;

  public MongoProductRepositoryAdapter(final MongoTemplate mongoTemplate, final ProductDocumentMapper mapper) {
    this.mongoTemplate = mongoTemplate;
    this.mapper = mapper;
  }

  @Override
  public List<Product> findAll() {
    return mongoTemplate.findAll(ProductDocument.class).stream()
      .map(mapper::toDomain)
      .toList();
  }

  @Override
  @Cacheable(cacheNames = "productCache", key = "'maxSales'")
  public OptionalInt findMaxSalesUnits() {
    final var doc = mongoTemplate.findOne(
      new Query().with(Sort.by(Sort.Direction.DESC, "salesUnits"))
        .limit(1),
      ProductDocument.class);
    return doc != null ? OptionalInt.of(doc.salesUnits()) : OptionalInt.empty();
  }

  @Override
  @Cacheable(cacheNames = "productCache", key = "'scoreables'")
  public List<ScoreableProduct> findAllScoreable() {
    final var query = new Query();
    query.fields().include("_id", "salesUnits", "stock");
    final var docs = mongoTemplate.find(query, Document.class, "products");
    return docs.stream().map(this::toScoreable).toList();
  }

  @Override
  public List<Product> findByIds(final List<ProductId> ids) {
    final var stringIds = ids.stream().map(id -> String.valueOf(id.value())).toList();
    return mongoTemplate.find(
      Query.query(where("_id").in(stringIds)),
      ProductDocument.class
    ).stream()
      .map(mapper::toDomain)
      .toList();
  }

  private ScoreableProduct toScoreable(final Document doc) {
    @SuppressWarnings("unchecked")
    final var stockEntries = (List<Document>) doc.get("stock");
    final var sizesWithStock = stockEntries != null
      ? stockEntries.stream().filter(e -> ((Number) e.get("quantity")).intValue() > 0).count()
      : 0L;
    final var totalSizes = stockEntries != null ? stockEntries.size() : 1;
    final var ratio = (double) sizesWithStock / totalSizes;

    return new ScoreableProduct(
      ProductId.of(Long.parseLong(doc.getObjectId("_id").toString())),
      doc.getInteger("salesUnits", 0),
      ratio
    );
  }
}
