package dev.jpje.productsorter.adapter.persistence.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;

/**
 * Pins the cache tier identity: the in-process manager must always be L1 and the Redis manager L2,
 * no matter how the beans are declared or discovered. If positional {@code List} wiring is ever
 * restored and a manager is inserted before Caffeine, these identity assertions fail.
 */
@ExtendWith(MockitoExtension.class)
class CacheConfigTest {

  private final CacheConfig cacheConfig = new CacheConfig();

  @Mock
  private ObjectProvider<CacheManager> l2Provider;

  @Test
  void shouldPinL1ToCaffeineAndL2ToRedis() {
    final var caffeine = cacheConfig.caffeineCacheManager();
    final var redisCache = new ConcurrentMapCache("productCache");
    when(l2Provider.getIfAvailable()).thenReturn(cacheManager(redisCache));

    final var cache = cacheConfig.multiTierCacheManager(
      caffeine, l2Provider, CircuitBreakerRegistry.ofDefaults()).getCache("productCache");

    assertThat(cache).isInstanceOf(MultiTierCache.class);
    final var multiTier = (MultiTierCache) cache;
    assertThat(multiTier.l1())
      .as("L1 is the in-process Caffeine cache")
      .isSameAs(caffeine.getCache("productCache"));
    assertThat(multiTier.l2())
      .as("L2 is the resilient Redis cache")
      .isInstanceOf(ResilientCache.class);
    assertThat(multiTier.l2().getNativeCache())
      .as("The resilient wrapper wraps the Redis cache, not the in-process one")
      .isSameAs(redisCache.getNativeCache());
  }

  @Test
  void shouldMemoiseTheResolvedCacheAcrossCalls() {
    when(l2Provider.getIfAvailable()).thenReturn(cacheManager(new ConcurrentMapCache("productCache")));
    final var manager = cacheConfig.multiTierCacheManager(
      cacheConfig.caffeineCacheManager(), l2Provider, CircuitBreakerRegistry.ofDefaults());

    assertThat(manager.getCache("productCache"))
      .as("getCache returns a stable instance across cache operations")
      .isSameAs(manager.getCache("productCache"));
  }

  @Test
  void shouldDegradeToL1OnlyWhenRedisIsDisabled() {
    final var caffeine = cacheConfig.caffeineCacheManager();
    when(l2Provider.getIfAvailable()).thenReturn(null);

    final var cache = cacheConfig.multiTierCacheManager(
      caffeine, l2Provider, CircuitBreakerRegistry.ofDefaults()).getCache("productCache");

    assertThat(cache)
      .as("Without Redis the in-process cache is served directly, unwrapped")
      .isSameAs(caffeine.getCache("productCache"));
  }

  private static SimpleCacheManager cacheManager(final ConcurrentMapCache cache) {
    final var manager = new SimpleCacheManager();
    manager.setCaches(List.of(cache));
    manager.afterPropertiesSet();
    return manager;
  }
}
