package dev.jpje.productsorter.adapter.persistence.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;

@ExtendWith(MockitoExtension.class)
class MultiTierCacheTest {

  private static final String KEY = "k";
  private static final String VALUE = "v";

  @Mock
  private Cache l1;

  @Mock
  private Cache l2;

  private MultiTierCache multiTierCache;

  @BeforeEach
  void setUp() {
    multiTierCache = new MultiTierCache("t", l1, l2);
  }

  @Test
  void shouldReturnName() {
    assertThat(multiTierCache.getName()).as("delegated cache name").isEqualTo("t");
  }

  @Test
  void shouldReturnNativeCache() {
    assertThat(multiTierCache.getNativeCache()).as("native cache is the composite itself").isSameAs(multiTierCache);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("reads")
  void shouldReadFromL1(final ReadOp op) {
    op.stubHit().accept(l1);
    assertThat(op.read().apply(multiTierCache)).as("L1 hit returns value without touching L2").isEqualTo(VALUE);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("reads")
  void shouldFallBackToL2AndPromote(final ReadOp op) {
    op.stubHit().accept(l2);
    assertThat(op.read().apply(multiTierCache)).as("L2 fallback returns value").isEqualTo(VALUE);
    verify(l1).put(KEY, VALUE);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("missingReads")
  void shouldReturnNullWhenBothTiersMiss(final Function<MultiTierCache, Object> read) {
    assertThat(read.apply(multiTierCache)).as("both-tier miss returns null").isNull();
  }

  @Test
  void shouldLoadAndPopulateBothTiersOnMiss() {
    assertThat(multiTierCache.get(KEY, () -> "loaded")).as("loader result returned on miss").isEqualTo("loaded");
    verify(l1).put(KEY, "loaded");
    verify(l2).put(KEY, "loaded");
  }

  @Test
  void shouldPropagateLoaderException() {
    assertThatThrownBy(() -> multiTierCache.get(KEY, () -> {
      throw new RuntimeException("fail");
    })).as("loader failure wrapped").isInstanceOf(Cache.ValueRetrievalException.class);
  }

  @ParameterizedTest(name = "put value={0}")
  @NullSource
  @ValueSource(strings = "x")
  void shouldPutToBothTiers(final String value) {
    multiTierCache.put(KEY, value);
    verify(l1).put(KEY, value);
    verify(l2).put(KEY, value);
  }

  @Test
  void shouldEvictFromBothTiers() {
    multiTierCache.evict(KEY);
    verify(l1).evict(KEY);
    verify(l2).evict(KEY);
  }

  @Test
  void shouldClearBothTiers() {
    multiTierCache.clear();
    verify(l1).clear();
    verify(l2).clear();
  }

  private static Stream<Arguments> reads() {
    return Stream.of(
      arguments(named("get(key)", new ReadOp(
        tier -> when(tier.get(KEY)).thenReturn(new SimpleValueWrapper(VALUE)),
        cache -> Objects.requireNonNull(cache.get(KEY)).get()))),
      arguments(named("get(key, type)", new ReadOp(
        tier -> when(tier.get(KEY, String.class)).thenReturn(VALUE),
        cache -> cache.get(KEY, String.class)))),
      arguments(named("get(key, loader)", new ReadOp(
        tier -> when(tier.get(KEY)).thenReturn(new SimpleValueWrapper(VALUE)),
        cache -> cache.get(KEY, () -> "loaded")))));
  }

  private static Stream<Arguments> missingReads() {
    return Stream.of(
      arguments(named("get(key)", (Function<MultiTierCache, Object>) cache -> cache.get(KEY))),
      arguments(named("get(key, type)", (Function<MultiTierCache, Object>) cache -> cache.get(KEY, String.class))));
  }

  private record ReadOp(Consumer<Cache> stubHit, Function<MultiTierCache, Object> read) {
  }
}
