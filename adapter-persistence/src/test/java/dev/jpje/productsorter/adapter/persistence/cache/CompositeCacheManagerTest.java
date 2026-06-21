package dev.jpje.productsorter.adapter.persistence.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@ExtendWith(MockitoExtension.class)
class CompositeCacheManagerTest {

  @Mock
  private CacheManager managerA;

  @Mock
  private CacheManager managerB;

  @Test
  void shouldRejectNull() {
    assertThatThrownBy(() -> new CompositeCacheManager(null))
      .as("Should reject null cache managers")
      .isInstanceOf(NullPointerException.class);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("getCacheCases")
  void shouldGetCache(final List<CacheManager> managers, final String name, final Class<?> expectedType) {
    final var composite = new CompositeCacheManager(managers);
    final var result = composite.getCache(name);
    if (expectedType == null) {
      assertThat(result)
        .as("Should return null for unknown cache")
        .isNull();
    } else {
      assertThat(result)
        .as("Cache type should match")
        .isInstanceOf(expectedType);
    }
  }

  @Test
  void shouldReturnEmptyNamesWhenEmpty() {
    assertThat(new CompositeCacheManager(List.of()).getCacheNames())
      .as("Should return empty names when no managers")
      .isEmpty();
  }

  @Test
  void shouldCollectUniqueNames() {
    when(managerA.getCacheNames()).thenReturn(Set.of("x", "y"));
    when(managerB.getCacheNames()).thenReturn(Set.of("y", "z"));
    assertThat(new CompositeCacheManager(List.of(managerA, managerB)).getCacheNames())
      .as("Should collect unique names from all managers")
      .containsExactlyInAnyOrder("x", "y", "z");
  }

  private static Stream<Arguments> getCacheCases() {
    return Stream.of(
      arguments(named("unknown name", List.of()), "unknown", null),
      arguments(named("single manager", List.of(newCacheManager("single", mock(Cache.class)))), "single", Cache.class),
      arguments(named("first miss then hit", List.of(newCacheManager("x", null), newCacheManager("y", mock(Cache.class)))), "y", Cache.class),
      arguments(named("two sources", List.of(newCacheManager("s", mock(Cache.class)), newCacheManager("s", mock(Cache.class)))), "s", MultiTierCache.class));
  }

  private static CacheManager newCacheManager(final String name, final Cache c) {
    final var cm = mock(CacheManager.class);
    when(cm.getCache(name)).thenReturn(c);
    return cm;
  }
}
