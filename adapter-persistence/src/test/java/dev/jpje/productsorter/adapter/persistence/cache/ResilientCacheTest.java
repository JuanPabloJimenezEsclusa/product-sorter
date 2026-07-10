package dev.jpje.productsorter.adapter.persistence.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;

@ExtendWith(MockitoExtension.class)
class ResilientCacheTest {

  private static final String KEY = "k";
  private static final String VALUE = "v";
  private static final RuntimeException REDIS_DOWN = new RuntimeException("redis down");

  @Mock
  private Cache delegate;

  private CircuitBreaker circuitBreaker;
  private ResilientCache resilientCache;

  @BeforeEach
  void setUp() {
    circuitBreaker = CircuitBreaker.of("redis", CircuitBreakerConfig.ofDefaults());
    resilientCache = new ResilientCache(delegate, circuitBreaker);
  }

  @Test
  void shouldDelegateName() {
    when(delegate.getName()).thenReturn("productCache");
    assertThat(resilientCache.getName()).as("name delegated to L2 cache").isEqualTo("productCache");
  }

  @Test
  void shouldDelegateNativeCache() {
    when(delegate.getNativeCache()).thenReturn("native");
    assertThat(resilientCache.getNativeCache()).as("native cache delegated to L2 cache").isEqualTo("native");
  }

  @Test
  void shouldReturnValueWhenDelegateHealthy() {
    when(delegate.get(KEY)).thenReturn(new SimpleValueWrapper(VALUE));
    assertThat(resilientCache.get(KEY))
      .as("healthy L2 cache returns the wrapped value")
      .isNotNull()
      .extracting(Cache.ValueWrapper::get)
      .isEqualTo(VALUE);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("degradingReads")
  void shouldDegradeToMissWhenDelegateThrows(final ReadOp op) {
    op.stubFailure().accept(delegate);
    assertThat(op.read().apply(resilientCache)).as("failed L2 read degrades to a cache miss").isNull();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("failingWrites")
  void shouldSwallowWriteFailures(final WriteOp op) {
    op.stubFailure().accept(delegate);
    assertThatCode(() -> op.invoke().accept(resilientCache)).as("failed L2 write is swallowed").doesNotThrowAnyException();
  }

  @Test
  void shouldLoadValueOnMissWithLoader() {
    when(delegate.get(KEY)).thenReturn(null);
    assertThat(resilientCache.get(KEY, () -> "loaded")).as("loader result is returned on miss").isEqualTo("loaded");
    verify(delegate).put(KEY, "loaded");
  }

  @Test
  void shouldReturnCachedValueWithoutLoading() {
    when(delegate.get(KEY)).thenReturn(new SimpleValueWrapper("cached"));
    assertThat(resilientCache.get(KEY, () -> "loaded")).as("cached value returned without invoking loader").isEqualTo("cached");
    verify(delegate, never()).put(any(), any());
  }

  @Test
  void shouldFailFastWhenCircuitOpen() {
    circuitBreaker.transitionToOpenState();
    assertThat(resilientCache.get(KEY)).as("open circuit returns a miss").isNull();
    verify(delegate, never()).get(any());
  }

  private static Stream<Arguments> degradingReads() {
    return Stream.of(
      arguments(named("get(key)", new ReadOp(
        d -> when(d.get(KEY)).thenThrow(REDIS_DOWN),
        cache -> cache.get(KEY)))),
      arguments(named("get(key, type)", new ReadOp(
        d -> when(d.get(KEY, String.class)).thenThrow(REDIS_DOWN),
        cache -> cache.get(KEY, String.class)))));
  }

  private static Stream<Arguments> failingWrites() {
    return Stream.of(
      arguments(named("put", new WriteOp(
        d -> doThrow(REDIS_DOWN).when(d).put(any(), any()),
        cache -> cache.put(KEY, VALUE)))),
      arguments(named("evict", new WriteOp(
        d -> doThrow(REDIS_DOWN).when(d).evict(any()),
        cache -> cache.evict(KEY)))),
      arguments(named("clear", new WriteOp(
        d -> doThrow(REDIS_DOWN).when(d).clear(),
        ResilientCache::clear))));
  }

  private record ReadOp(Consumer<Cache> stubFailure, Function<ResilientCache, Object> read) {
  }

  private record WriteOp(Consumer<Cache> stubFailure, Consumer<ResilientCache> invoke) {
  }
}
