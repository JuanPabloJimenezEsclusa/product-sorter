package dev.jpje.productsorter.chaos;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.mongodb.client.MongoClients;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import dev.jpje.productsorter.adapter.persistence.mongo.entity.ProductDocument;
import dev.jpje.productsorter.adapter.persistence.mongo.entity.StockEntry;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

@Tag("chaos")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("chaos")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JwksChaosTest {

  private static final int OK = 200;
  private static final int UNAUTHORIZED = 401;
  private static final String KID = "chaos-key";
  private static final KeyPair RSA_KEY = generateRsaKey();
  private static final AtomicBoolean JWKS_UP = new AtomicBoolean(true);
  private static final AtomicInteger TRANSIENT_FAILURES = new AtomicInteger();
  private static final MockWebServer JWKS_SERVER = startJwksServer();

  @Container
  static MongoDBContainer mongodb = new MongoDBContainer("mongodb/mongodb-community-server:8.3.4-ubi9");

  @DynamicPropertySource
  static void configure(final DynamicPropertyRegistry registry) {
    registry.add("spring.mongodb.uri", mongodb::getReplicaSetUrl);
    registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
      () -> JWKS_SERVER.url("/jwks").toString());
  }

  @LocalServerPort
  private int port;

  @BeforeEach
  void resetState() {
    JWKS_UP.set(true);
    TRANSIENT_FAILURES.set(0);
    seedProducts();
  }

  @Test
  @Order(1)
  void shouldRecoverAuthenticationAfterTransientJwksFailure() throws Exception {
    TRANSIENT_FAILURES.set(1);

    assertThat(sortStatus(signedJwt(KID)))
      .as("resilient decoder retries a transient JWKS fetch failure and authenticates").isEqualTo(OK);
    assertThat(TRANSIENT_FAILURES.get())
      .as("the transient JWKS failure was consumed by a retried fetch").isZero();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("authScenarios")
  @Order(2)
  void shouldResolveAuthenticationOutcome(final AuthCase authCase) throws Exception {
    JWKS_UP.set(authCase.jwksReachable());

    assertThat(sortStatus(signedJwt(authCase.kid())))
      .as("authentication outcome").isEqualTo(authCase.expectedStatus());
  }

  @Test
  @Order(3)
  void shouldKeepAuthenticatingAfterJwksGoesDown() throws Exception {
    final var token = signedJwt(KID);
    assertThat(sortStatus(token)).as("warm-up caches the signing key").isEqualTo(OK);

    JWKS_UP.set(false);
    assertThat(sortStatus(token))
      .as("cached JWKS keeps authentication working during a JWKS outage").isEqualTo(OK);
  }

  private static Stream<Arguments> authScenarios() {
    return Stream.of(
      arguments(named("valid key with JWKS reachable -> 200", new AuthCase(true, KID, OK))),
      arguments(named("unresolvable key with JWKS down -> 401", new AuthCase(false, "unknown-key", UNAUTHORIZED))));
  }

  private static MockWebServer startJwksServer() {
    try {
      final var jwks = new JWKSet(new RSAKey.Builder((RSAPublicKey) RSA_KEY.getPublic())
        .keyID(KID).keyUse(KeyUse.SIGNATURE).algorithm(JWSAlgorithm.RS256).build()).toString();
      final var server = new MockWebServer();
      server.setDispatcher(new Dispatcher() {
        @Override
        public @NonNull MockResponse dispatch(final @NonNull RecordedRequest request) {
          if (!JWKS_UP.get()) {
            return new MockResponse().setResponseCode(503);
          }
          if (TRANSIENT_FAILURES.getAndUpdate(remaining -> remaining > 0 ? remaining - 1 : 0) > 0) {
            return new MockResponse().setResponseCode(503);
          }
          return new MockResponse().setResponseCode(200)
            .setHeader("Content-Type", "application/json").setBody(jwks);
        }
      });
      server.start();
      return server;
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private static String signedJwt(final String kid) throws Exception {
    final var now = Instant.now(Clock.systemUTC());
    final var claims = new JWTClaimsSet.Builder()
      .issuer("http://localhost:9999/realms/test")
      .subject("chaos-test-user")
      .issueTime(Date.from(now))
      .expirationTime(Date.from(now.plusSeconds(3600)))
      .claim("realm_access", Map.of("roles", List.of("admin", "operator")))
      .build();
    final var signedJwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(kid).build(), claims);
    signedJwt.sign(new RSASSASigner(RSA_KEY.getPrivate()));
    return signedJwt.serialize();
  }

  private static KeyPair generateRsaKey() {
    try {
      final var generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      return generator.generateKeyPair();
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private void seedProducts() {
    final var client = MongoClients.create(mongodb.getReplicaSetUrl());
    final var template = new MongoTemplate(client, "test");
    template.dropCollection("products");
    template.save(new ProductDocument("1", "V-NECH BASIC SHIRT", 100,
      List.of(new StockEntry("S", 4), new StockEntry("M", 9), new StockEntry("L", 0)), null, 0.667));

    client.close();
  }

  private int sortStatus(final String token) {
    return given()
      .port(port).auth().oauth2(token).contentType("application/json")
      .body(Map.of("weights", Map.of("salesUnits", 0.7, "stockRatio", 0.3)))
      .queryParam("size", 5)
      .when().post("/api/v1/products/sort")
      .then().extract().statusCode();
  }

  private record AuthCase(boolean jwksReachable, String kid, int expectedStatus) {
  }
}
