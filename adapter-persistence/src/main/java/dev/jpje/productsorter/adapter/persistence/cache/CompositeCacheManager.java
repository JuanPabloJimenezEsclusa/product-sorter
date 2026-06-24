package dev.jpje.productsorter.adapter.persistence.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

public class CompositeCacheManager implements CacheManager {

  private final List<CacheManager> cacheManagers;

  public CompositeCacheManager(final List<CacheManager> cacheManagers) {
    this.cacheManagers = Objects.requireNonNull(cacheManagers);
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
    return new MultiTierCache(name, caches.getFirst(), caches.getLast());
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