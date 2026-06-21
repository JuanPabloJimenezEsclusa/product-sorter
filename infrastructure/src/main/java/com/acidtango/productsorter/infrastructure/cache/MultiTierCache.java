package com.acidtango.productsorter.infrastructure.cache;

import java.util.concurrent.Callable;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;

public class MultiTierCache implements Cache {

  private final String name;
  private final Cache l1;
  private final Cache l2;

  public MultiTierCache(final String name, final Cache l1, final Cache l2) {
    this.name = name;
    this.l1 = l1;
    this.l2 = l2;
  }

  @Override
  public @NonNull String getName() {
    return name;
  }

  @Override
  public @NonNull Object getNativeCache() {
    return this;
  }

  @Override
  public @Nullable ValueWrapper get(final @NonNull Object key) {
    var wrapper = l1.get(key);
    if (wrapper != null) {
      return wrapper;
    }
    wrapper = l2.get(key);
    if (wrapper != null) {
      l1.put(key, wrapper.get());
      return wrapper;
    }
    return null;
  }

  @Override
  public @Nullable <T> T get(final @NonNull Object key, final @Nullable Class<T> type) {
    var value = l1.get(key, type);
    if (value != null) {
      return value;
    }
    value = l2.get(key, type);
    if (value != null) {
      l1.put(key, value);
    }
    return value;
  }

  @Override
  @SuppressWarnings("unchecked")
  public @Nullable <T> T get(final @NonNull Object key, final @NonNull Callable<T> valueLoader) {
    var cached = l1.get(key);
    if (cached != null) {
      return (T) cached.get();
    }
    cached = l2.get(key);
    if (cached != null) {
      final var value = (T) cached.get();
      l1.put(key, value);
      return value;
    }
    try {
      final var loaded = valueLoader.call();
      put(key, loaded);
      return loaded;
    } catch (final Exception e) {
      throw new ValueRetrievalException(key, valueLoader, e);
    }
  }

  @Override
  public void put(final @NonNull Object key, final @Nullable Object value) {
    l1.put(key, value);
    l2.put(key, value);
  }

  @Override
  public void evict(final @NonNull Object key) {
    l1.evict(key);
    l2.evict(key);
  }

  @Override
  public void clear() {
    l1.clear();
    l2.clear();
  }
}