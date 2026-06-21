package com.acidtango.productsorter.e2e;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.acidtango.productsorter.ProductSorterApplication;
import com.acidtango.productsorter.infrastructure.persistence.SpringDataMongoProductRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.stream.Stream;

@SpringBootTest(
  classes = ProductSorterApplication.class,
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ProductSortingE2ETest {

  @Container
  static MongoDBContainer mongodb = new MongoDBContainer("mongo:7");

  @DynamicPropertySource
  static void configureProperties(final DynamicPropertyRegistry registry) {
    registry.add("spring.data.mongodb.uri", mongodb::getReplicaSetUrl);
    registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
      () -> "http://localhost:8081/realms/product-sorter");
  }

  @LocalServerPort
  private int port;

  @Autowired
  private SpringDataMongoProductRepository springRepo;

  @BeforeEach
  void setUp() {
    RestAssured.port = port;
    springRepo.deleteAll();
  }

  @Test
  void shouldReturn401WithoutJwt() {
    final var response = given()
      .contentType(ContentType.JSON)
      .body(Map.of("weights", Map.of("salesUnits", 1.0, "stockRatio", 0.0)))
      .when()
      .post("/api/v1/products/sort")
      .then()
      .extract()
      .response();
    assertThat(response.statusCode())
      .as("Should return 401 without JWT")
      .isEqualTo(401);
  }

  @Test
  void shouldReturn400ForEmptyWeights() {
    final var response = given()
      .contentType(ContentType.JSON)
      .body(Map.of("weights", Map.of()))
      .when()
      .post("/api/v1/products/sort")
      .then()
      .extract()
      .response();
    assertThat(response.statusCode())
      .as("Should return 400 or 401 for empty weights")
      .isIn(400, 401);
  }

  @ParameterizedTest
  @MethodSource("weightScenarios")
  void shouldReturnUnauthorizedForAllWeightVariants(final Map<String, Double> weights) {
    final var response = given()
      .contentType(ContentType.JSON)
      .body(Map.of("weights", weights))
      .when()
      .post("/api/v1/products/sort")
      .then()
      .extract()
      .response();
    assertThat(response.statusCode())
      .as("Should return 401 without JWT for any weights")
      .isEqualTo(401);
  }

  private static Stream<Arguments> weightScenarios() {
    return Stream.of(
      Arguments.of(Map.of("salesUnits", 1.0, "stockRatio", 0.0)),
      Arguments.of(Map.of("salesUnits", 0.7, "stockRatio", 0.3)),
      Arguments.of(Map.of("salesUnits", 0.5, "stockRatio", 0.5)));
  }
}
