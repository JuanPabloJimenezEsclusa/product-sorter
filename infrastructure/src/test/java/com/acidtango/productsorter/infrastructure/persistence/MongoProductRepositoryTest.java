package com.acidtango.productsorter.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.acidtango.productsorter.domain.model.*;
import com.acidtango.productsorter.infrastructure.persistence.entity.ProductDocument;
import com.acidtango.productsorter.infrastructure.persistence.entity.ProductDocument.StockEntry;
import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.stream.Stream;

@Testcontainers
class MongoProductRepositoryTest {

  @Container
  static MongoDBContainer mongodb = new MongoDBContainer("mongo:7");

  private MongoTemplate mongoTemplate;

  @BeforeEach
  void setUp() {
    final var client = MongoClients.create(mongodb.getReplicaSetUrl());
    mongoTemplate = new MongoTemplate(client, "test");
    mongoTemplate.dropCollection("products");
  }

  @ParameterizedTest
  @MethodSource("productDocuments")
  void shouldPersistAndRetrieveProductDocument(final ProductDocument doc) {
    mongoTemplate.save(doc, "products");
    final var result = mongoTemplate.findById(doc.getId(), ProductDocument.class, "products");
    assertThat(result)
      .as("Document should be persisted")
      .isNotNull();
    assertThat(result.getName())
      .as("Document name should match")
      .isEqualTo(doc.getName());
    assertThat(result.getSalesUnits())
      .as("Document sales units should match")
      .isEqualTo(doc.getSalesUnits());
  }

  @Test
  void shouldMapProductNameFromDocument() {
    mongoTemplate.save(new ProductDocument("1", "V-NECK BASIC SHIRT", 100,
      List.of(new StockEntry("S", 4), new StockEntry("M", 9), new StockEntry("L", 0))), "products");
    final var doc = mongoTemplate.findById("1", ProductDocument.class, "products");
    assertThat(doc).as("Document should exist").isNotNull();
    final var product = domainFrom(doc);
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
    final var product = domainFrom(doc);
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
    final var product = domainFrom(doc);
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

  private static Product domainFrom(final ProductDocument doc) {
    return new Product(
      ProductId.of(Long.parseLong(doc.getId())),
      ProductName.of(doc.getName()),
      SalesUnits.of(doc.getSalesUnits()),
      Stock.of(doc.getStock().stream()
        .map(e -> StockBySize.of(Size.valueOf(e.getSize()), e.getQuantity()))
        .toList()));
  }

  private static Stream<Arguments> productDocuments() {
    return Stream.of(
      Arguments.of(new ProductDocument("1", "V-NECK BASIC SHIRT", 100,
        List.of(new StockEntry("S", 4), new StockEntry("M", 9), new StockEntry("L", 0)))),
      Arguments.of(new ProductDocument("5", "CONTRASTING LACE T-SHIRT", 650,
        List.of(new StockEntry("S", 0), new StockEntry("M", 1), new StockEntry("L", 0)))));
  }
}
