package com.acidtango.productsorter.infrastructure.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.AbstractCacheManager;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class CompositeCacheManager extends AbstractCacheManager {

  private final List<CacheManager> cacheManagers;

  public CompositeCacheManager(final List<CacheManager> cacheManagers) {
    this.cacheManagers = Objects.requireNonNull(cacheManagers);
  }

  @Override
  protected Collection<? extends Cache> loadCaches() {
    return cacheManagers.stream()
      .flatMap(cm -> cm.getCacheNames().stream()
        .map(cm::getCache)
        .filter(Objects::nonNull))
      .toList();
  }

  @Override
  protected Cache getMissingCache(final String name) {
    for (final var cm : cacheManagers) {
      final var cache = cm.getCache(name);
      if (cache != null) {
        return cache;
      }
    }
    return null;
  }
}
