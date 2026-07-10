package dev.jpje.productsorter.contract;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.mongodb.client.MongoClients;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import dev.jpje.productsorter.adapter.persistence.mongo.entity.ProductDocument;
import dev.jpje.productsorter.adapter.persistence.mongo.entity.StockEntry;
import dev.jpje.productsorter.domain.vo.CursorCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
class ProductApiContractTest {

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
  static class ContractTestJwtConfig {
    @Bean
    @Primary
    JwtDecoder jwtDecoder() {
      return NimbusJwtDecoder.withPublicKey((java.security.interfaces.RSAPublicKey) RSA_KEY.getPublic()).build();
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

  @Test
  void sortResponseShouldMatchOpenApiSpec() {
    given()
      .port(port).auth().oauth2(jwt).contentType("application/json")
      .body(Map.of("weights", Map.of("salesUnits", 0.7, "stockRatio", 0.3)))
    .when()
      .post("/api/v1/products/sort?size=20")
    .then()
      .statusCode(200)
      .body(matchesJsonSchemaInClasspath("schema/product-page.json"))
      .body("data", not(empty()));
  }

  @Test
  void listResponseShouldMatchOpenApiSpec() {
    given()
      .port(port).auth().oauth2(jwt).contentType("application/json")
    .when()
      .get("/api/v1/products?size=20")
    .then()
      .statusCode(200)
      .body(matchesJsonSchemaInClasspath("schema/product-page.json"))
      .body("data", not(empty()));
  }

  @Test
  void errorResponseShouldMatchOpenApiSpec() {
    given()
      .port(port).auth().oauth2(jwt).contentType("application/json")
      .body(Map.of("weights", Map.of()))
    .when()
      .post("/api/v1/products/sort")
    .then()
      .statusCode(400)
      .body(matchesJsonSchemaInClasspath("schema/error-response.json"));
  }

  @Test
  void emptyListResponseShouldMatchOpenApiSpec() {
    given()
      .port(port).auth().oauth2(jwt).contentType("application/json")
    .when()
      .get("/api/v1/products?cursor=" + CursorCodec.encode(0, "z") + "&size=20")
    .then()
      .statusCode(200)
      .body(matchesJsonSchemaInClasspath("schema/product-page.json"))
      .body("data", empty());
  }

  private void seedProducts() {
    final var client = MongoClients.create(mongodb.getReplicaSetUrl());
    final var template = new MongoTemplate(client, "test");
    template.dropCollection("products");

    template.save(product("1", "V-NECH BASIC SHIRT", 100,
      List.of(new StockEntry("S", 4), new StockEntry("M", 9), new StockEntry("L", 0))));
    template.save(product("5", "CONTRASTING LACE T-SHIRT", 650,
      List.of(new StockEntry("S", 0), new StockEntry("M", 1), new StockEntry("L", 0))));

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
      .subject("contract-test-user")
      .issueTime(Date.from(now))
      .expirationTime(Date.from(now.plusSeconds(Integer.MAX_VALUE)))
      .claim("realm_access", Map.of("roles", List.of("admin", "operator")))
      .build();
    final var signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
    signedJwt.sign(new RSASSASigner(RSA_KEY.getPrivate()));
    return signedJwt.serialize();
  }
}
