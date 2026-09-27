package dev.jpje.productsorter.application.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PageSizeTest {

  @Test
  void shouldDefaultWhenAbsent() {
    assertThat(PageSize.resolve(null)).as("absent size defaults").isEqualTo(PageSize.DEFAULT);
  }

  @Test
  void shouldExposeContractBounds() {
    assertThat(PageSize.DEFAULT).as("default page size").isEqualTo(20);
    assertThat(PageSize.MIN).as("minimum page size").isEqualTo(1);
    assertThat(PageSize.MAX).as("maximum page size").isEqualTo(100);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 20, 100})
  void shouldAcceptSizesWithinBounds(final int size) {
    assertThat(PageSize.resolve(size)).as("in-range size is returned unchanged").isEqualTo(size);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 0, 101, Integer.MAX_VALUE})
  void shouldRejectSizesOutsideBounds(final int size) {
    assertThatThrownBy(() -> PageSize.resolve(size))
      .as("out-of-range size is rejected, not clamped")
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("page size");
  }
}
