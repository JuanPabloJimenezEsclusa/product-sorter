package dev.jpje.productsorter.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.mongodb.client.MongoClients;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import dev.jpje.productsorter.adapter.persistence.mongo.entity.ProductDocument;
import dev.jpje.productsorter.adapter.persistence.mongo.entity.StockEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ProductSorterE2eTest {

  private static final KeyPair RSA_KEY = generateRsaKey();

  private static KeyPair generateRsaKey() {
    try {
      final var gen = KeyPairGenerator.getInstance("RSA");
      gen.initialize(2048);
      return gen.generateKeyPair();
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @TestConfiguration
  static class E2eTestJwtConfig {
    @Bean
    @Primary
    JwtDecoder jwtDecoder() {
      return NimbusJwtDecoder.withPublicKey((java.security.interfaces.RSAPublicKey) RSA_KEY.getPublic()).build();
    }
  }

  @Container
  static MongoDBContainer mongodb = new MongoDBContainer("mongodb/mongodb-community-server:8.3.2-ubi9");

  @DynamicPropertySource
  static void configure(final DynamicPropertyRegistry registry) {
    registry.add("spring.mongodb.uri", mongodb::getReplicaSetUrl);
  }

  @LocalServerPort
  private int port;

  private String jwt;

  @BeforeEach
  void setUp() throws Exception {
    seedProducts();
    jwt = generateJwt();
  }

  private void seedProducts() {
    final var client = MongoClients.create(mongodb.getReplicaSetUrl());
    final var template = new MongoTemplate(client, "test");
    template.dropCollection("products");

    template.save(new ProductDocument("1", "V-NECH BASIC SHIRT", 100,
      List.of(new StockEntry("S", 4), new StockEntry("M", 9), new StockEntry("L", 0))));
    template.save(new ProductDocument("2", "CONTRASTING FABRIC T-SHIRT", 50,
      List.of(new StockEntry("S", 35), new StockEntry("M", 9), new StockEntry("L", 9))));
    template.save(new ProductDocument("3", "RAISED PRINT T-SHIRT", 80,
      List.of(new StockEntry("S", 20), new StockEntry("M", 2), new StockEntry("L", 20))));
    template.save(new ProductDocument("4", "PLEATED T-SHIRT", 3,
      List.of(new StockEntry("S", 25), new StockEntry("M", 30), new StockEntry("L", 10))));
    template.save(new ProductDocument("5", "CONTRASTING LACE T-SHIRT", 650,
      List.of(new StockEntry("S", 0), new StockEntry("M", 1), new StockEntry("L", 0))));
    template.save(new ProductDocument("6", "SLOGAN T-SHIRT", 20,
      List.of(new StockEntry("S", 9), new StockEntry("M", 2), new StockEntry("L", 5))));

    client.close();
  }

  private static String generateJwt() throws Exception {
    final var clock = Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC);
    final var now = Instant.now(clock);
    final var claims = new JWTClaimsSet.Builder()
      .issuer("http://localhost:9999/realms/test")
      .subject("e2e-test-user")
      .issueTime(Date.from(now))
      .expirationTime(Date.from(now.plusSeconds(Integer.MAX_VALUE)))
      .build();
    final var signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
    signedJwt.sign(new RSASSASigner(RSA_KEY.getPrivate()));
    return signedJwt.serialize();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("sortScenarios")
  void shouldSort(final SortCase c) {
    var res = given()
      .port(port).auth().oauth2(jwt).contentType("application/json")
      .body(Map.of("weights", c.weights()))
      .when()
      .post("/api/v1/products/sort?page=" + c.page() + "&size=" + c.size())
      .then()
      .statusCode(200)
      .body("page", equalTo(c.expectedPage()))
      .body("size", equalTo(c.expectedSize()));
    if (c.expectedSize() > 1) {
      res = res.body("data", hasSize(Math.min(c.size(), 6)));
    }
    for (final var assertion : c.bodyAssertions()) {
      res = res.body(assertion.path(), equalTo(assertion.expected()));
    }
  }

  @Test
  void shouldReturn400ForEmptyWeights() {
    given()
      .port(port)
      .auth().oauth2(jwt)
      .contentType("application/json")
      .body(Map.of("weights", Map.of()))
      .when()
      .post("/api/v1/products/sort?page=1&size=20")
      .then()
      .statusCode(400);
  }

  @Test
  void shouldReturn401WithoutToken() {
    given()
      .port(port)
      .contentType("application/json")
      .body(Map.of("weights", Map.of("salesUnits", 0.7, "stockRatio", 0.3)))
      .when()
      .post("/api/v1/products/sort")
      .then()
      .statusCode(401);
  }

  private record BodyAssertion(String path, Object expected) {
  }

  private record SortCase(Map<String, Double> weights, int page, int size, int expectedPage,
                          int expectedSize,
                          List<BodyAssertion> bodyAssertions) {
  }

  private static Stream<Arguments> sortScenarios() {
    return Stream.of(
      arguments(named("sales only", new SortCase(
        Map.of("salesUnits", 1.0, "stockRatio", 0.0), 1, 20, 1, 20,
        List.of(
          new BodyAssertion("data[0].product.id", "5"),
          new BodyAssertion("data[0].product.salesUnits", 650),
          new BodyAssertion("data[1].product.id", "1"),
          new BodyAssertion("data[1].product.salesUnits", 100))))),
      arguments(named("stock only", new SortCase(
        Map.of("salesUnits", 0.0, "stockRatio", 1.0), 1, 20, 1, 20,
        List.of(
          new BodyAssertion("data[0].product.id", "2"),
          new BodyAssertion("data[1].product.id", "3"))))),
      arguments(named("paginated", new SortCase(
        Map.of("salesUnits", 0.5, "stockRatio", 0.5), 1, 2, 1, 2,
        List.of()))));
  }
}
