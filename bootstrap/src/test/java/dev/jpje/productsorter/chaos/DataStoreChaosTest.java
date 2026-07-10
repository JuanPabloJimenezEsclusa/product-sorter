package dev.jpje.productsorter.chaos;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

import com.mongodb.client.MongoClients;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.redis.testcontainers.RedisContainer;
import dev.jpje.productsorter.adapter.persistence.mongo.entity.ProductDocument;
import dev.jpje.productsorter.adapter.persistence.mongo.entity.StockEntry;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.Toxic;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

@Tag("chaos")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("chaos")
@Testcontainers
class DataStoreChaosTest {

  private static final int OK = 200;
  private static final int SERVICE_UNAVAILABLE = 503;
  private static final Duration TIMEOUT = Duration.ofSeconds(20);

  private static final KeyPair RSA_KEY = generateRsaKey();
  private static final Network NETWORK = Network.newNetwork();

  @Container
  static MongoDBContainer mongodb = new MongoDBContainer("mongodb/mongodb-community-server:8.3.4-ubi9")
    .withNetwork(NETWORK).withNetworkAliases("mongo").withExposedPorts(27017);

  @Container
  static RedisContainer redis = new RedisContainer("redis:8-alpine")
    .withNetwork(NETWORK).withNetworkAliases("redis").withExposedPorts(6379);

  @Container
  static ToxiproxyContainer toxiProxy = new ToxiproxyContainer("ghcr.io/shopify/toxiproxy:2.12.0")
    .withNetwork(NETWORK).withExposedPorts(8474, 8666, 8667);

  private static Proxy mongoProxy;
  private static Proxy redisProxy;

  @BeforeAll
  static void setUp() {
    try {
      final var client = new ToxiproxyClient(toxiProxy.getHost(), toxiProxy.getControlPort());
      mongoProxy = client.createProxy(mongodb.getNetworkAliases().getFirst(), "0.0.0.0:8666", "mongo:27017");
      redisProxy = client.createProxy(redis.getNetworkAliases().getFirst(), "0.0.0.0:8667", "redis:6379");
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @DynamicPropertySource
  static void properties(final DynamicPropertyRegistry registry) {
    registry.add("spring.mongodb.uri", () -> "mongodb://" + toxiProxy.getHost() + ":" + toxiProxy.getMappedPort(8666)
      + "/productsorter?directConnection=true&connectTimeoutMS=1000&serverSelectionTimeoutMS=1000&timeoutMS=1500");
    registry.add("spring.data.redis.host", toxiProxy::getHost);
    registry.add("spring.data.redis.port", () -> toxiProxy.getMappedPort(8667));
  }

  @TestConfiguration
  static class ChaosJwtConfig {
    @Bean
    @Primary
    JwtDecoder jwtDecoder() {
      return NimbusJwtDecoder.withPublicKey((RSAPublicKey) RSA_KEY.getPublic()).build();
    }
  }

  @LocalServerPort
  private int port;

  @Autowired
  private CircuitBreakerRegistry circuitBreakerRegistry;

  @Autowired
  private CacheManager cacheManager;

  private final AtomicInteger weightSeq = new AtomicInteger();
  private String jwt;

  @BeforeEach
  void resetState() throws Exception {
    restore(mongoProxy);
    restore(redisProxy);
    circuitBreakerRegistry.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
    final var cache = cacheManager.getCache("productCache");
    if (cache != null) {
      cache.clear();
    }
    seedProducts();
    jwt = generateJwt();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("mongoFaults")
  void shouldFailFastThenRecoverOnMongoFault(final FaultInjector fault) throws IOException {
    fault.inject(mongoProxy);
    await().atMost(TIMEOUT).until(() -> uniqueSortStatus() == SERVICE_UNAVAILABLE);

    restore(mongoProxy);
    await().atMost(TIMEOUT).until(() -> uniqueSortStatus() == OK);
  }

  @ParameterizedTest(name = "{0} stays available when Redis is down")
  @MethodSource("readEndpoints")
  void shouldServeFromMongoWhenRedisDown(final ToIntFunction<DataStoreChaosTest> endpoint) throws IOException {
    redisProxy.disable();
    assertThat(endpoint.applyAsInt(this)).as("served from MongoDB despite Redis outage").isEqualTo(OK);
  }

  @Test
  void shouldStayAvailableWhileRedisFlaps() throws IOException {
    redisProxy.disable();
    assertThat(uniqueSortStatus()).as("available with Redis down").isEqualTo(OK);

    redisProxy.enable();
    assertThat(uniqueSortStatus()).as("available with Redis restored").isEqualTo(OK);

    redisProxy.disable();
    assertThat(uniqueSortStatus()).as("available when Redis drops again").isEqualTo(OK);
  }

  private static Stream<Arguments> mongoFaults() {
    return Stream.of(
      arguments(named("latency beyond operation timeout",
        (FaultInjector) proxy -> proxy.toxics().latency("mongo-latency", ToxicDirection.DOWNSTREAM, 4000))),
      arguments(named("connection outage", (FaultInjector) Proxy::disable)));
  }

  private static Stream<Arguments> readEndpoints() {
    return Stream.of(
      arguments(named("sort", (ToIntFunction<DataStoreChaosTest>) DataStoreChaosTest::uniqueSortStatus)),
      arguments(named("list", (ToIntFunction<DataStoreChaosTest>) DataStoreChaosTest::listStatus)));
  }

  private int uniqueSortStatus() {
    final double sales = 0.2 + (weightSeq.incrementAndGet() % 500) / 1000.0;
    return given()
      .port(port).auth().oauth2(jwt).contentType("application/json")
      .body(Map.of("weights", Map.of("salesUnits", sales, "stockRatio", 1.0 - sales)))
      .queryParam("size", 5)
      .when().post("/api/v1/products/sort")
      .then().extract().statusCode();
  }

  private int listStatus() {
    return given()
      .port(port).auth().oauth2(jwt)
      .queryParam("size", 5)
      .when().get("/api/v1/products")
      .then().extract().statusCode();
  }

  private static void restore(final Proxy proxy) throws IOException {
    for (final Toxic toxic : proxy.toxics().getAll()) {
      toxic.remove();
    }
    proxy.enable();
  }

  private void seedProducts() {
    final var client = MongoClients.create(mongodb.getReplicaSetUrl());
    final var template = new MongoTemplate(client, "productsorter");
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
    final long withStock = stock.stream().filter(entry -> entry.quantity() > 0).count();
    final double stockRatio = stock.isEmpty() ? 0.0 : (double) withStock / stock.size();
    return new ProductDocument(id, name, salesUnits, stock, null, stockRatio);
  }

  private static String generateJwt() throws Exception {
    final var now = Instant.now(Clock.systemUTC());
    final var claims = new JWTClaimsSet.Builder()
      .issuer("http://localhost:9999/realms/test")
      .subject("chaos-test-user")
      .issueTime(Date.from(now))
      .expirationTime(Date.from(now.plusSeconds(3600)))
      .claim("realm_access", Map.of("roles", List.of("admin", "operator")))
      .build();
    final var signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
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

  @FunctionalInterface
  private interface FaultInjector {
    void inject(Proxy proxy) throws IOException;
  }
}
