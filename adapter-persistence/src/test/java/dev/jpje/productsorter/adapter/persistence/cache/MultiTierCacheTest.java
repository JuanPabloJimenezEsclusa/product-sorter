package dev.jpje.productsorter.adapter.persistence.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;

@ExtendWith(MockitoExtension.class)
class MultiTierCacheTest {

  @Mock
  private Cache l1;

  @Mock
  private Cache l2;

  private MultiTierCache cache;

  @BeforeEach
  void setUp() {
    cache = new MultiTierCache("t", l1, l2);
  }

  @Test
  void shouldReturnName() {
    assertThat(cache.getName()).isEqualTo("t");
  }

  @Test
  void shouldReturnNativeCache() {
    assertThat(cache.getNativeCache()).isSameAs(cache);
  }

  @Test
  void shouldGetFromL1() {
    when(l1.get("k")).thenReturn(new SimpleValueWrapper("v"));
    final var result = cache.get("k");
    assertThat(result).isNotNull();
    assertThat(result.get()).isEqualTo("v");
  }

  @Test
  void shouldFallbackToL2() {
    when(l2.get("k")).thenReturn(new SimpleValueWrapper("v"));
    final var result = cache.get("k");
    assertThat(result).isNotNull();
    assertThat(result.get()).isEqualTo("v");
    verify(l1).put("k", "v");
  }

  @Test
  void shouldReturnNullWhenBothMiss() {
    assertThat(cache.get("k")).isNull();
  }

  @Test
  void shouldGetTypedFromL1() {
    when(l1.get("k", String.class)).thenReturn("v");
    assertThat(cache.get("k", String.class)).isEqualTo("v");
  }

  @Test
  void shouldFallbackTypedToL2() {
    when(l2.get("k", String.class)).thenReturn("v");
    assertThat(cache.get("k", String.class)).isEqualTo("v");
    verify(l1).put("k", "v");
  }

  @Test
  void shouldReturnNullWhenBothMissTyped() {
    assertThat(cache.get("k", String.class)).isNull();
  }

  @Test
  void shouldGetWithLoaderFromL1() {
    when(l1.get("k")).thenReturn(new SimpleValueWrapper("v"));
    assertThat(cache.get("k", () -> "loaded")).isEqualTo("v");
  }

  @Test
  void shouldGetWithLoaderFromL2() {
    when(l2.get("k")).thenReturn(new SimpleValueWrapper("v"));
    assertThat(cache.get("k", () -> "loaded")).isEqualTo("v");
    verify(l1).put("k", "v");
  }

  @Test
  void shouldGetWithLoaderFromLoader() {
    assertThat(cache.get("k", () -> "loaded")).isEqualTo("loaded");
    verify(l1).put("k", "loaded");
    verify(l2).put("k", "loaded");
  }

  @Test
  void shouldPropagateLoaderException() {
    assertThatThrownBy(() -> cache.get("k", () -> {
      throw new RuntimeException("fail");
    })).isInstanceOf(Cache.ValueRetrievalException.class);
  }

  @Test
  void shouldPut() {
    cache.put("k", "x");
    verify(l1).put("k", "x");
    verify(l2).put("k", "x");
  }

  @Test
  void shouldPutNull() {
    cache.put("k", null);
    verify(l1).put("k", null);
    verify(l2).put("k", null);
  }

  @Test
  void shouldEvict() {
    cache.evict("k");
    verify(l1).evict("k");
    verify(l2).evict("k");
  }

  @Test
  void shouldClear() {
    cache.clear();
    verify(l1).clear();
    verify(l2).clear();
  }
}
