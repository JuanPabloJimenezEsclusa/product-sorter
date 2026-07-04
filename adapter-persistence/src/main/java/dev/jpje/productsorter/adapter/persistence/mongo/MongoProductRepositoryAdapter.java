package dev.jpje.productsorter.adapter.persistence.mongo;

import java.util.List;

import dev.jpje.productsorter.adapter.persistence.mongo.entity.ProductDocument;
import dev.jpje.productsorter.domain.model.AppliedWeights;
import dev.jpje.productsorter.domain.model.Product;
import dev.jpje.productsorter.domain.port.ProductRepository;
import dev.jpje.productsorter.domain.vo.CursorCodec;
import org.bson.Document;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class MongoProductRepositoryAdapter implements ProductRepository {

  private static final String COLLECTION_NAME = "products";
  private final MongoTemplate mongoTemplate;
  private final ProductDocumentMapper mapper;

  public MongoProductRepositoryAdapter(final MongoTemplate mongoTemplate, final ProductDocumentMapper mapper) {
    this.mongoTemplate = mongoTemplate;
    this.mapper = mapper;
  }

  @Override
  @Cacheable(
    cacheNames = "productCache",
    key = "'page-' + #limit",
    condition = "#encodedCursor == null")
  public PagedResult findPage(final String encodedCursor, final int limit) {
    final var query = new Query()
      .with(Sort.by(Sort.Direction.ASC, "_id"))
      .limit(limit);
    if (encodedCursor != null) {
      query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("_id")
        .gt(CursorCodec.decode(encodedCursor).productId()));
    }
    final var products = mongoTemplate.find(query, ProductDocument.class, COLLECTION_NAME).stream()
      .map(mapper::toDomain)
      .toList();

    String nextCursor = null;
    if (!products.isEmpty()) {
      final var last = products.getLast();
      nextCursor = CursorCodec.encode(0, last.productId().value());
    }

    return new PagedResult(products, nextCursor);
  }

  @Override
  @Cacheable(
    cacheNames = "productCache",
    key = "#weights.salesUnitsWeight() + '-' + #weights.stockWeight() + '-' + #limit",
    condition = "#encodedCursor == null")
  public PagedResult sortByWeights(final AppliedWeights weights, final String encodedCursor, final int limit) {
    final var aggregation = ProductSorterHelper.buildAggregation(weights, encodedCursor, limit);

    final var results = mongoTemplate.aggregate(aggregation, COLLECTION_NAME, Document.class)
      .getMappedResults();

    String nextCursor = null;
    if (!results.isEmpty()) {
      final var lastDoc = results.getLast();
      final var lastScore = lastDoc.get("weightedScore", Number.class).doubleValue();
      final var lastId = lastDoc.get("_id").toString();
      nextCursor = CursorCodec.encode(lastScore, lastId);
    }

    final var products = results.stream()
      .map(this::toProductWithScore)
      .toList();

    return new PagedResult(products, nextCursor);
  }

  private Product toProductWithScore(final Document doc) {
    final var salesUnits = doc.getInteger("salesUnits", 0);
    final var name = doc.getString("name");
    final var id = doc.get("_id").toString();
    final double weightedScore = doc.get("weightedScore", Number.class).doubleValue();

    @SuppressWarnings("unchecked")
    final var stockEntries = (List<Document>) doc.get("stock");

    final var productDoc = new ProductDocument(id, name, salesUnits,
      stockEntries != null ? stockEntries.stream()
        .map(e -> new dev.jpje.productsorter.adapter.persistence.mongo.entity.StockEntry(
          e.getString("size"), e.getInteger("quantity", 0)))
        .toList() : List.of(),
      weightedScore);

    return mapper.toDomain(productDoc);
  }
}
