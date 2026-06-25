package dev.jpje.productsorter.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
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
import org.hamcrest.Matchers;
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
      return NimbusJwtDecoder.withPublicKey((RSAPublicKey) RSA_KEY.getPublic()).build();
    }
  }

  @Container
  static MongoDBContainer mongodb = new MongoDBContainer("mongodb/mongodb-community-server:8.3.4-ubi9");

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

  @ParameterizedTest(name = "{0}")
  @MethodSource("sortScenarios")
  void shouldSort(final SortCase c) {
    var req = given()
      .port(port).auth().oauth2(jwt).contentType("application/json")
      .body(Map.of("weights", c.weights()));
    if (c.cursor() != null) {
      req = req.queryParam("cursor", c.cursor());
    }
    var res = req.queryParam("size", c.size())
      .when()
      .post("/api/v1/products/sort")
      .then()
      .statusCode(200);
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
      .post("/api/v1/products/sort?size=20")
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

  @Test
  void shouldPaginateWithCursor() {
    final var firstPage = given()
      .port(port).auth().oauth2(jwt).contentType("application/json")
      .body(Map.of("weights", Map.of("salesUnits", 1.0, "stockRatio", 0.0)))
      .queryParam("size", 2)
      .when()
      .post("/api/v1/products/sort")
      .then()
      .statusCode(200)
      .body("data", hasSize(2))
      .body("nextCursor", Matchers.notNullValue())
      .extract();

    final var cursor = firstPage.path("nextCursor");

    given()
      .port(port).auth().oauth2(jwt).contentType("application/json")
      .body(Map.of("weights", Map.of("salesUnits", 1.0, "stockRatio", 0.0)))
      .queryParam("cursor", cursor)
      .queryParam("size", 2)
      .when()
      .post("/api/v1/products/sort")
      .then()
      .statusCode(200)
      .body("data", hasSize(2));
  }

  private void seedProducts() {
    final var client = MongoClients.create(mongodb.getReplicaSetUrl());
    final var template = new MongoTemplate(client, "test");
    template.dropCollection("products");

    template.save(product("1", "V-NECH BASIC SHIRT", 100,
      List.of(new StockEntry("S", 4), new StockEntry("M", 9), new StockEntry("L", 0))));
    template.save(product("2", "CONTRASTING FABRIC T-SHIRT", 50,
      List.of(new StockEntry("S", 35), new StockEntry("M", 9), new StockEntry("L", 9))));
    template.save(product("3", "RAISED PRINT T-SHIRT", 80,
      List.of(new StockEntry("S", 20), new StockEntry("M", 2), new StockEntry("L", 20))));
    template.save(product("4", "PLEATED T-SHIRT", 3,
      List.of(new StockEntry("S", 25), new StockEntry("M", 30), new StockEntry("L", 10))));
    template.save(product("5", "CONTRASTING LACE T-SHIRT", 650,
      List.of(new StockEntry("S", 0), new StockEntry("M", 1), new StockEntry("L", 0))));
    template.save(product("6", "SLOGAN T-SHIRT", 20,
      List.of(new StockEntry("S", 9), new StockEntry("M", 2), new StockEntry("L", 5))));

    client.close();
  }

  private static ProductDocument product(final String id, final String name, final int salesUnits,
                                          final List<StockEntry> stock) {
    final long withStock = stock.stream().filter(e -> e.quantity() > 0).count();
    final double stockRatio = stock.isEmpty() ? 0.0 : (double) withStock / stock.size();
    return new ProductDocument(id, name, salesUnits, stock, null, stockRatio);
  }

  private static String generateJwt() throws Exception {
    final var clock = Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC);
    final var now = Instant.now(clock);
    final var claims = new JWTClaimsSet.Builder()
      .issuer("http://localhost:9999/realms/test")
      .subject("e2e-test-user")
      .issueTime(Date.from(now))
      .expirationTime(Date.from(now.plusSeconds(Integer.MAX_VALUE)))
      .claim("realm_access", Map.of("roles", List.of("admin", "operator")))
      .build();
    final var signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
    signedJwt.sign(new RSASSASigner(RSA_KEY.getPrivate()));
    return signedJwt.serialize();
  }

  private static Stream<Arguments> sortScenarios() {
    return Stream.of(
      arguments(named("sales only", new SortCase(
        Map.of("salesUnits", 1.0, "stockRatio", 0.0), null, 20,
        List.of(
          new BodyAssertion("data[0].id", "5"),
          new BodyAssertion("data[0].salesUnits", 650),
          new BodyAssertion("data[1].id", "1"),
          new BodyAssertion("data[1].salesUnits", 100))))),
      arguments(named("stock only", new SortCase(
        Map.of("salesUnits", 0.0, "stockRatio", 1.0), null, 20,
        List.of(
          new BodyAssertion("data[0].id", "6"),
          new BodyAssertion("data[1].id", "4"))))));
  }

  private record BodyAssertion(String path, Object expected) {
  }

  private record SortCase(Map<String, Double> weights, String cursor, int size,
                          List<BodyAssertion> bodyAssertions) {
  }
}
