package dev.jpje.productsorter.adapter.persistence.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import com.mongodb.client.MongoClients;
import dev.jpje.productsorter.adapter.persistence.mongo.entity.ProductDocument;
import dev.jpje.productsorter.adapter.persistence.mongo.entity.StockEntry;
import dev.jpje.productsorter.domain.model.AppliedWeights;
import dev.jpje.productsorter.domain.model.Product;
import dev.jpje.productsorter.domain.vo.CursorCodec;
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
  void shouldPage(final String cursor, final int limit, final int expectedCount, final String firstId) {
    final var result = repository.findPage(cursor, limit);
    assertThat(result.products())
      .as("Page should have %d items", expectedCount)
      .hasSize(expectedCount);
    if (!result.products().isEmpty()) {
      assertThat(result.products().getFirst().productId().value())
        .as("First item ID should match")
        .isEqualTo(firstId);
    }
    assertThat(result.total())
      .as("Total should be 6")
      .isEqualTo(6);
  }

  @Test
  void shouldPageWithCursor() {
    final var page1 = repository.findPage(null, 3);
    assertThat(page1.products()).hasSize(3);
    assertThat(page1.products().getFirst().productId().value()).isEqualTo("1");
    assertThat(page1.total()).isEqualTo(6);
    assertThat(page1.nextCursor()).isNotNull();

    final var page2 = repository.findPage(page1.nextCursor(), 3);
    assertThat(page2.products()).hasSize(3);
    assertThat(page2.products().getFirst().productId().value()).isEqualTo("4");
    assertThat(page2.total()).isEqualTo(6);
  }

  @Test
  void shouldSortByWeightsSalesOnly() {
    final var weights = new AppliedWeights(1.0, 0.0);
    final var result = repository.sortByWeights(weights, null, 20);
    assertThat(result.products())
      .as("Should return all 6 products")
      .hasSize(6);
    assertThat(result.products())
      .as("Should be sorted by score descending")
      .extracting(Product::weightedScore)
      .isSortedAccordingTo(Comparator.reverseOrder());
    assertThat(result.products().getFirst().productId().value())
      .as("Product 5 has highest sales")
      .isEqualTo("5");
    assertThat(result.total())
      .as("Should count 6 products")
      .isEqualTo(6);
  }

  @Test
  void shouldSortByWeightsStockOnly() {
    final var weights = new AppliedWeights(0.0, 1.0);
    final var result = repository.sortByWeights(weights, null, 20);
    assertThat(result.products())
      .as("Should return all 6 products")
      .hasSize(6);
    assertThat(result.products().getFirst().productId().value())
      .as("Product with highest stock ratio (tiebreaker _id desc)")
      .isEqualTo("6");
  }

  @Test
  void shouldSortByWeightsCursorPaginated() {
    final var weights = new AppliedWeights(0.7, 0.3);
    final var page1 = repository.sortByWeights(weights, null, 2);
    assertThat(page1.products()).hasSize(2);
    assertThat(page1.nextCursor()).isNotNull();
    assertThat(page1.products().getFirst().productId().value())
      .isEqualTo("5");

    final var page2 = repository.sortByWeights(weights, page1.nextCursor(), 2);
    assertThat(page2.products()).hasSize(2);
    assertThat(page2.nextCursor()).isNotNull();
    assertThat(page2.products().getFirst().productId().value())
      .isNotEqualTo("5");
  }

  @Test
  void shouldReturnEmptyForCursorPastEnd() {
    final var weights = new AppliedWeights(1.0, 0.0);
    final var empty = repository.sortByWeights(weights, CursorCodec.encode(-1, "z"), 20);
    assertThat(empty.products()).isEmpty();
    assertThat(empty.total()).isGreaterThan(0);
    assertThat(empty.nextCursor()).isNull();
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
    assertThat(doc).isNotNull();
    assertThat(mapper.toDomain(doc).stock().stockRatio())
      .as("Stock ratio should match expected")
      .isCloseTo(0.667, within(0.01));
  }

  private void seedProducts() {
    mongoTemplate.save(new ProductDocument("1", "ALPHA SHIRT", 100, List.of(
      new StockEntry("S", 4), new StockEntry("M", 9), new StockEntry("L", 0)), null));
    mongoTemplate.save(new ProductDocument("2", "BETA T-SHIRT", 50, List.of(
      new StockEntry("S", 35), new StockEntry("M", 9), new StockEntry("L", 9)), null));
    mongoTemplate.save(new ProductDocument("3", "GAMMA POLO", 80, List.of(
      new StockEntry("S", 20), new StockEntry("M", 2), new StockEntry("L", 20)), null));
    mongoTemplate.save(new ProductDocument("4", "DELTA HOODIE", 3, List.of(
      new StockEntry("S", 25), new StockEntry("M", 30), new StockEntry("L", 10)), null));
    mongoTemplate.save(new ProductDocument("5", "LACE SHIRT", 650, List.of(
      new StockEntry("S", 0), new StockEntry("M", 1), new StockEntry("L", 0)), null));
    mongoTemplate.save(new ProductDocument("6", "SLOGAN TEE", 20, List.of(
      new StockEntry("S", 9), new StockEntry("M", 2), new StockEntry("L", 5)), null));
  }

  private static Stream<Arguments> paginationCases() {
    return Stream.of(
      arguments(named("first page", (String) null), 3, 3, "1"),
      arguments(named("first page small", (String) null), 2, 2, "1"));
  }

  private static Stream<Arguments> toDomainCases() {
    return Stream.of(
      arguments(named("product 1", "1"), "ALPHA SHIRT", 100),
      arguments(named("product 5", "5"), "LACE SHIRT", 650));
  }
}
