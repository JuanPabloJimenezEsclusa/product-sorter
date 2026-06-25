package dev.jpje.productsorter.adapter.persistence.cache;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;

public class ResilientCache implements Cache {

  private static final Logger log = LoggerFactory.getLogger(ResilientCache.class);

  private final Cache delegate;
  private final CircuitBreaker circuitBreaker;

  public ResilientCache(final Cache delegate, final CircuitBreaker circuitBreaker) {
    this.delegate = delegate;
    this.circuitBreaker = circuitBreaker;
  }

  @Override
  public @NonNull String getName() {
    return delegate.getName();
  }

  @Override
  public @NonNull Object getNativeCache() {
    return delegate.getNativeCache();
  }

  @Override
  public @Nullable ValueWrapper get(final @NonNull Object key) {
    return read(() -> delegate.get(key));
  }

  @Override
  public @Nullable <T> T get(final @NonNull Object key, final @Nullable Class<T> type) {
    return read(() -> delegate.get(key, type));
  }

  @Override
  public @Nullable <T> T get(final @NonNull Object key, final @NonNull Callable<T> valueLoader) {
    final var cached = read(() -> delegate.get(key));
    if (cached != null) {
      @SuppressWarnings("unchecked")
      final var value = (T) cached.get();
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
    write(() -> delegate.put(key, value));
  }

  @Override
  public void evict(final @NonNull Object key) {
    write(() -> delegate.evict(key));
  }

  @Override
  public void clear() {
    write(delegate::clear);
  }

  private <T> @Nullable T read(final Supplier<T> supplier) {
    try {
      return circuitBreaker.executeSupplier(supplier);
    } catch (final RuntimeException ex) {
      log.warn("L2 cache '{}' read degraded to miss: {}", delegate.getName(), ex.getMessage());
      return null;
    }
  }

  private void write(final Runnable action) {
    try {
      circuitBreaker.executeRunnable(action);
    } catch (final RuntimeException ex) {
      log.warn("L2 cache '{}' write skipped: {}", delegate.getName(), ex.getMessage());
    }
  }
}
