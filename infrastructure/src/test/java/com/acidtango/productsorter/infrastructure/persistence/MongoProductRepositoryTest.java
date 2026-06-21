package com.acidtango.productsorter.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.stream.Stream;

import com.acidtango.productsorter.infrastructure.persistence.entity.ProductDocument;
import com.acidtango.productsorter.infrastructure.persistence.entity.StockEntry;
import com.mongodb.client.MongoClients;
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
  static MongoDBContainer mongodb = new MongoDBContainer("mongodb/mongodb-community-server:8-ubi9");

  private final ProductDocumentMapper mapper = new ProductDocumentMapper();
  private MongoTemplate mongoTemplate;

  @BeforeEach
  void setUp() {
    final var client = MongoClients.create(mongodb.getReplicaSetUrl());
    mongoTemplate = new MongoTemplate(client, "test");
    mongoTemplate.dropCollection("products");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("productDocuments")
  void shouldPersistAndRetrieveProductDocument(final ProductDocument doc) {
    mongoTemplate.save(doc, "products");
    final var result = mongoTemplate.findById(doc.id(), ProductDocument.class, "products");
    assertThat(result)
      .as("Document should be persisted")
      .isNotNull();
    assertThat(result.name())
      .as("Document name should match")
      .isEqualTo(doc.name());
    assertThat(result.salesUnits())
      .as("Document sales units should match")
      .isEqualTo(doc.salesUnits());
  }

  @Test
  void shouldMapProductNameFromDocument() {
    mongoTemplate.save(new ProductDocument("1", "V-NECK BASIC SHIRT", 100,
      List.of(new StockEntry("S", 4), new StockEntry("M", 9), new StockEntry("L", 0))), "products");
    final var doc = mongoTemplate.findById("1", ProductDocument.class, "products");
    assertThat(doc).as("Document should exist").isNotNull();
    final var product = mapper.toDomain(doc);
    assertThat(product.productName().value())
      .as("Product name should map correctly")
      .isEqualTo("V-NECK BASIC SHIRT");
  }

  @Test
  void shouldMapSalesUnitsFromDocument() {
    mongoTemplate.save(new ProductDocument("1", "V-NECK BASIC SHIRT", 100,
      List.of(new StockEntry("S", 4), new StockEntry("M", 9), new StockEntry("L", 0))), "products");
    final var doc = mongoTemplate.findById("1", ProductDocument.class, "products");
    assertThat(doc).as("Document should exist").isNotNull();
    final var product = mapper.toDomain(doc);
    assertThat(product.salesUnits().value())
      .as("Sales units should map correctly")
      .isEqualTo(100);
  }

  @Test
  void shouldMapStockRatioFromDocument() {
    mongoTemplate.save(new ProductDocument("1", "V-NECK BASIC SHIRT", 100,
      List.of(new StockEntry("S", 4), new StockEntry("M", 9), new StockEntry("L", 0))), "products");
    final var doc = mongoTemplate.findById("1", ProductDocument.class, "products");
    assertThat(doc).as("Document should exist").isNotNull();
    final var product = mapper.toDomain(doc);
    assertThat(product.stock().ratio())
      .as("Stock ratio should map and compute correctly")
      .isCloseTo(0.667, within(0.001));
  }

  @Test
  void shouldReturnEmptyCollectionWhenNoProducts() {
    final var products = mongoTemplate.findAll(ProductDocument.class, "products");
    assertThat(products)
      .as("Should be empty when no products exist")
      .isEmpty();
  }

  private static Stream<Arguments> productDocuments() {
    return Stream.of(
      arguments(named("V-NECK", new ProductDocument("1", "V-NECK BASIC SHIRT", 100,
        List.of(new StockEntry("S", 4), new StockEntry("M", 9), new StockEntry("L", 0))))),
      arguments(named("LACE", new ProductDocument("5", "CONTRASTING LACE T-SHIRT", 650,
        List.of(new StockEntry("S", 0), new StockEntry("M", 1), new StockEntry("L", 0))))));
  }
}
