package dev.jpje.productsorter.adapter.persistence.mongo;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.List;
import java.util.OptionalInt;

import dev.jpje.productsorter.adapter.persistence.mongo.entity.ProductDocument;
import dev.jpje.productsorter.domain.model.Product;
import dev.jpje.productsorter.domain.model.ScoreableProduct;
import dev.jpje.productsorter.domain.port.ProductRepository;
import dev.jpje.productsorter.domain.vo.ProductId;
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
  @Cacheable(cacheNames = "productCache", key = "#page + '-' + #size")
  public List<Product> findPage(final int page, final int size) {
    if (page < 1 || size < 1) {
      return List.of();
    }
    final var skip = ((long) page - 1) * size;
    return mongoTemplate.find(
      new Query().with(Sort.by(Sort.Direction.ASC, "_id")).skip(skip).limit(size),
      ProductDocument.class, "products").stream()
      .map(mapper::toDomain)
      .toList();
  }

  @Override
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
    final var docs = mongoTemplate.find(query, org.bson.Document.class, "products");
    return docs.stream().map(mapper::toScoreable).toList();
  }

  @Override
  public List<Product> findByIds(final List<ProductId> ids) {
    final var stringIds = ids.stream().map(ProductId::value).toList();
    return mongoTemplate.find(
      Query.query(where("_id").in(stringIds)),
      ProductDocument.class
    ).stream()
      .map(mapper::toDomain)
      .toList();
  }
}
