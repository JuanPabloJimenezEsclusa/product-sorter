package dev.jpje.productsorter.adapter.persistence.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

public class CompositeCacheManager implements CacheManager {

  private final List<CacheManager> cacheManagers;
  private final CircuitBreaker l2CircuitBreaker;

  public CompositeCacheManager(final List<CacheManager> cacheManagers) {
    this(cacheManagers, null);
  }

  public CompositeCacheManager(final List<CacheManager> cacheManagers,
                               final CircuitBreaker l2CircuitBreaker) {
    this.cacheManagers = Objects.requireNonNull(cacheManagers);
    this.l2CircuitBreaker = l2CircuitBreaker;
  }

  @Override
  public @Nullable Cache getCache(final @NonNull String name) {
    final var caches = new ArrayList<Cache>();
    for (final var cm : cacheManagers) {
      final var cache = cm.getCache(name);
      if (cache != null) {
        caches.add(cache);
      }
    }
    if (caches.isEmpty()) {
      return null;
    }
    if (caches.size() == 1) {
      return caches.getFirst();
    }
    final var l2 = l2CircuitBreaker == null
      ? caches.getLast()
      : new ResilientCache(caches.getLast(), l2CircuitBreaker);
    return new MultiTierCache(name, caches.getFirst(), l2);
  }

  @Override
  public @NonNull Collection<String> getCacheNames() {
    final var names = new LinkedHashMap<String, Boolean>();
    for (final var cm : cacheManagers) {
      for (final var name : cm.getCacheNames()) {
        names.put(name, Boolean.TRUE);
      }
    }
    return names.keySet();
  }
}