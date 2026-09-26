package dev.jpje.productsorter.adapter.persistence.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;

class CompositeCacheManagerTest {

  @Test
  void shouldRejectNullL1Tier() {
    assertThatThrownBy(() -> new CompositeCacheManager(null, null, null))
      .as("The in-process L1 tier is mandatory")
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldReturnNullWhenNeitherTierResolvesTheCache() {
    final var composite = new CompositeCacheManager(cacheManager(), null, null);

    assertThat(composite.getCache("unknown"))
      .as("No tier declares the cache")
      .isNull();
  }

  @Test
  void shouldReturnL1CacheWhenOnlyL1IsPresent() {
    final var l1Cache = new ConcurrentMapCache("productCache");
    final var composite = new CompositeCacheManager(cacheManager(l1Cache), null, null);

    assertThat(composite.getCache("productCache"))
      .as("A single in-process tier is returned unwrapped")
      .isSameAs(l1Cache);
  }

  @Test
  void shouldReturnL2CacheWhenOnlyL2IsPresent() {
    final var l2Cache = new ConcurrentMapCache("productCache");
    final var composite = new CompositeCacheManager(cacheManager(), cacheManager(l2Cache), null);

    assertThat(composite.getCache("productCache"))
      .as("L2 is returned when L1 does not declare the cache")
      .isSameAs(l2Cache);
  }

  @Test
  void shouldBindExplicitTierRolesWhenBothArePresent() {
    final var l1Cache = new ConcurrentMapCache("productCache");
    final var l2Cache = new ConcurrentMapCache("productCache");
    final var composite = new CompositeCacheManager(cacheManager(l1Cache), cacheManager(l2Cache), breaker());

    final var cache = composite.getCache("productCache");

    assertThat(cache).isInstanceOf(MultiTierCache.class);
    final var multiTier = (MultiTierCache) cache;
    assertThat(multiTier.l1())
      .as("L1 is always the in-process cache, never a Redis cache by declaration order")
      .isSameAs(l1Cache);
    assertThat(multiTier.l2())
      .as("L2 is the resilient Redis tier")
      .isInstanceOf(ResilientCache.class);
    assertThat(multiTier.l2().getNativeCache())
      .as("The resilient wrapper wraps the Redis cache, not the in-process one")
      .isSameAs(l2Cache.getNativeCache());
  }

  @Test
  void shouldMemoiseTheResolvedCachePerName() {
    final var composite = new CompositeCacheManager(
      cacheManager(new ConcurrentMapCache("productCache")),
      cacheManager(new ConcurrentMapCache("productCache")),
      breaker());

    assertThat(composite.getCache("productCache"))
      .as("Repeated lookups return the same instance")
      .isSameAs(composite.getCache("productCache"));
  }

  @Test
  void shouldCollectUniqueNamesAcrossBothTiers() {
    final var composite = new CompositeCacheManager(
      cacheManager(new ConcurrentMapCache("x"), new ConcurrentMapCache("y")),
      cacheManager(new ConcurrentMapCache("y"), new ConcurrentMapCache("z")),
      breaker());

    assertThat(composite.getCacheNames())
      .as("Names from both tiers are unioned")
      .containsExactlyInAnyOrder("x", "y", "z");
  }

  @Test
  void shouldNotGuardL1WhenTheRedisBreakerIsOpen() {
    final var l1Cache = new ConcurrentMapCache("productCache");
    final var l2Cache = new ConcurrentMapCache("productCache");
    l2Cache.put("key", "l2-value");
    final var openBreaker = breaker();
    openBreaker.transitionToOpenState();

    final var composite = new CompositeCacheManager(cacheManager(l1Cache), cacheManager(l2Cache), openBreaker);
    final var cache = composite.getCache("productCache");

    assertThat(cache).isNotNull();
    assertThat(cache.get("key"))
      .as("The L2 lookup degrades to a miss while the Redis breaker is open")
      .isNull();

    cache.put("local", "local-value");
    assertThat(cache.get("local", String.class))
      .as("In-process L1 reads are unaffected by the open Redis breaker")
      .isEqualTo("local-value");
  }

  private static CacheManager cacheManager(final Cache... caches) {
    final var manager = new SimpleCacheManager();
    manager.setCaches(List.of(caches));
    manager.afterPropertiesSet();
    return manager;
  }

  private static CircuitBreaker breaker() {
    return CircuitBreakerRegistry.ofDefaults().circuitBreaker("redis");
  }
}
