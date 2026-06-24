package dev.jpje.productsorter.adapter.persistence.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Stream;

import com.mongodb.client.MongoClients;
import dev.jpje.productsorter.adapter.persistence.mongo.entity.ProductDocument;
import dev.jpje.productsorter.adapter.persistence.mongo.entity.StockEntry;
import dev.jpje.productsorter.domain.vo.ProductId;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

@Testcontainers
class MongoProductRepositoryTest {

  @Container
  static MongoDBContainer mongodb = new MongoDBContainer("mongodb/mongodb-community-server:8.3.2-ubi9");

  private MongoProductRepositoryAdapter repository;
  private ProductDocumentMapper mapper;
  private MongoTemplate mongoTemplate;

  @BeforeEach
  void setUp() {
    mapper = new ProductDocumentMapper();
    final var client = MongoClients.create(mongodb.getReplicaSetUrl());
    mongoTemplate = new MongoTemplate(client, "test");
    mongoTemplate.dropCollection("products");
    repository = new MongoProductRepositoryAdapter(mongoTemplate, mapper);
    seedProducts();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("paginationCases")
  void shouldPage(final int page, final int size, final int expectedCount, final String firstId) {
    final var result = repository.findPage(page, size);
    assertThat(result)
      .as("Page should have %d items", expectedCount)
      .hasSize(expectedCount);
    if (!result.isEmpty()) {
      assertThat(result.getFirst().productId().value())
        .as("First item ID should match")
        .isEqualTo(firstId);
    }
  }

  @Test
  void shouldFindMaxSalesUnits() {
    assertThat(repository.findMaxSalesUnits())
      .as("Max sales units should match")
      .isEqualTo(OptionalInt.of(650));
  }

  @Test
  void shouldReturnEmptyMaxSalesWhenNoProducts() {
    mongoTemplate.dropCollection("products");
    assertThat(repository.findMaxSalesUnits())
      .as("Max sales units should be empty when no products")
      .isEmpty();
  }

  @Test
  void shouldReturnAllScoreable() {
    final var scoreables = repository.findAllScoreable();
    assertThat(scoreables)
      .as("Should return all products")
      .hasSize(6);
    assertThat(scoreables.getFirst().productId())
      .as("First scoreable should have an ID")
      .isNotNull();
    assertThat(scoreables.getFirst().salesUnits())
      .as("First scoreable should have positive sales")
      .isPositive();
    assertThat(scoreables.getFirst().stockRatio())
      .as("Stock ratio should be between 0 and 1")
      .isBetween(0.0, 1.0);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("findByIdCases")
  void shouldFindByIds(final List<String> ids, final int expectedCount, final List<String> expectedIds) {
    final var products = repository.findByIds(ids.stream().map(ProductId::of).toList());
    assertThat(products)
      .as("Should find %d products", expectedCount)
      .hasSize(expectedCount);
    assertThat(products.stream().map(p -> p.productId().value()).toList())
      .as("Found IDs should match expected")
      .isEqualTo(expectedIds);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("toDomainCases")
  void shouldMapToDomain(final String docId, final String expectedName, final int expectedSales) {
    final var doc = mongoTemplate.findById(docId, ProductDocument.class, "products");
    assertThat(doc)
      .as("Document should exist")
      .isNotNull();
    final var product = mapper.toDomain(doc);
    assertThat(product.productId().value())
      .as("Product ID should match")
      .isEqualTo(docId);
    assertThat(product.productName().value())
      .as("Product name should match")
      .isEqualTo(expectedName);
    assertThat(product.salesUnits().value())
      .as("Sales units should match")
      .isEqualTo(expectedSales);
  }

  @Test
  void shouldMapStockRatio() {
    final var doc = mongoTemplate.findById("1", ProductDocument.class, "products");
    assertThat(doc)
      .as("Document should exist")
      .isNotNull();
    assertThat(mapper.toDomain(doc).stock().ratio())
      .as("Stock ratio should be computed correctly")
      .isCloseTo(0.667, within(0.01));
  }

  private record ScoreableDocument(Document input, int expectedSales, double expectedRatio) {
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("scoreableCases")
  void shouldMapScoreable(final ScoreableDocument c) {
    final var scoreable = mapper.toScoreable(c.input);
    assertThat(scoreable.salesUnits())
      .as("Sales units should match")
      .isEqualTo(c.expectedSales);
    assertThat(scoreable.stockRatio())
      .as("Stock ratio should match")
      .isCloseTo(c.expectedRatio, within(0.01));
  }

  private void seedProducts() {
    mongoTemplate.save(new ProductDocument("1", "ALPHA SHIRT", 100, List.of(
      new StockEntry("S", 4), new StockEntry("M", 9), new StockEntry("L", 0))));
    mongoTemplate.save(new ProductDocument("2", "BETA T-SHIRT", 50, List.of(
      new StockEntry("S", 35), new StockEntry("M", 9), new StockEntry("L", 9))));
    mongoTemplate.save(new ProductDocument("3", "GAMMA POLO", 80, List.of(
      new StockEntry("S", 20), new StockEntry("M", 2), new StockEntry("L", 20))));
    mongoTemplate.save(new ProductDocument("4", "DELTA HOODIE", 3, List.of(
      new StockEntry("S", 25), new StockEntry("M", 30), new StockEntry("L", 10))));
    mongoTemplate.save(new ProductDocument("5", "LACE SHIRT", 650, List.of(
      new StockEntry("S", 0), new StockEntry("M", 1), new StockEntry("L", 0))));
    mongoTemplate.save(new ProductDocument("6", "SLOGAN TEE", 20, List.of(
      new StockEntry("S", 9), new StockEntry("M", 2), new StockEntry("L", 5))));
  }

  private static Stream<Arguments> paginationCases() {
    return Stream.of(
      arguments(named("first page", 1), 2, 2, "1"),
      arguments(named("second page", 2), 2, 2, "3"),
      arguments(named("last page partial", 3), 2, 2, "5"),
      arguments(named("page out of range", 99), 20, 0, null));
  }

  private static Stream<Arguments> findByIdCases() {
    return Stream.of(
      arguments(named("existing ids", List.of("2", "5")), 2, List.of("2", "5")),
      arguments(named("non existing ids", List.of("99", "100")), 0, List.of()),
      arguments(named("empty list", List.of()), 0, List.of()));
  }

  private static Stream<Arguments> toDomainCases() {
    return Stream.of(
      arguments(named("product 1", "1"), "ALPHA SHIRT", 100),
      arguments(named("product 5", "5"), "LACE SHIRT", 650));
  }

  private static Stream<Arguments> scoreableCases() {
    return Stream.of(
      arguments(named("fully stocked", new ScoreableDocument(
        new Document("_id", "1").append("salesUnits", 100)
          .append("stock", List.of(
            new Document("size", "S").append("quantity", 5),
            new Document("size", "M").append("quantity", 10),
            new Document("size", "L").append("quantity", 15))),
        100, 1.0))),
      arguments(named("partial stock", new ScoreableDocument(
        new Document("_id", "2").append("salesUnits", 50)
          .append("stock", List.of(
            new Document("size", "S").append("quantity", 0),
            new Document("size", "M").append("quantity", 1),
            new Document("size", "L").append("quantity", 0))),
        50, 0.333))),
      arguments(named("all out", new ScoreableDocument(
        new Document("_id", "3").append("salesUnits", 0)
          .append("stock", List.of(
            new Document("size", "S").append("quantity", 0),
            new Document("size", "M").append("quantity", 0),
            new Document("size", "L").append("quantity", 0))),
        0, 0.0))),
      arguments(named("null stock", new ScoreableDocument(
        new Document("_id", "4").append("salesUnits", 30),
        30, 0.0))));
  }
}
