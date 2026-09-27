package dev.jpje.productsorter.adapter.persistence.cache;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

public class CompositeCacheManager implements CacheManager {

  private final CacheManager l1;
  private final @Nullable CacheManager l2;
  private final @Nullable CircuitBreaker l2CircuitBreaker;
  private final ConcurrentMap<String, Cache> resolvedCaches = new ConcurrentHashMap<>();

  public CompositeCacheManager(final CacheManager l1,
                               final @Nullable CacheManager l2,
                               final @Nullable CircuitBreaker l2CircuitBreaker) {
    this.l1 = Objects.requireNonNull(l1);
    this.l2 = l2;
    this.l2CircuitBreaker = l2CircuitBreaker;
  }

  @Override
  public @Nullable Cache getCache(final @NonNull String name) {
    return resolvedCaches.computeIfAbsent(name, this::resolveCache);
  }

  @Override
  public @NonNull Collection<String> getCacheNames() {
    final var names = new LinkedHashMap<String, Boolean>();
    for (final var name : l1.getCacheNames()) {
      names.put(name, Boolean.TRUE);
    }
    if (l2 != null) {
      for (final var name : l2.getCacheNames()) {
        names.put(name, Boolean.TRUE);
      }
    }
    return names.keySet();
  }

  private @Nullable Cache resolveCache(final String name) {
    final var l1Cache = l1.getCache(name);
    if (l2 == null) {
      return l1Cache;
    }
    final var l2Cache = l2.getCache(name);
    final var resilientL2 = getResilientL2(l2Cache);

    if (l1Cache == null) {
      return resilientL2;
    }
    if (resilientL2 == null) {
      return l1Cache;
    }
    return new MultiTierCache(name, l1Cache, resilientL2);
  }

  private @Nullable Cache getResilientL2(final Cache l2Cache) {
    if (l2Cache == null) {
      return null;
    }
    return l2CircuitBreaker == null ? l2Cache : new ResilientCache(l2Cache, l2CircuitBreaker);
  }
}
