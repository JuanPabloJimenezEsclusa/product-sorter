package dev.jpje.productsorter.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import dev.jpje.productsorter.application.port.PageSize;
import dev.jpje.productsorter.application.port.SortProductsRequest;
import dev.jpje.productsorter.domain.model.AppliedWeights;
import dev.jpje.productsorter.domain.model.Metrics;
import dev.jpje.productsorter.domain.port.ProductPage;
import dev.jpje.productsorter.domain.port.ProductRepository;
import org.junit.jupiter.api.Test;

/**
 * Pins the shared size-normalisation contract: the list and sort read paths must accept and reject the
 * same {@code size} values and report the same effective page size, because the rule lives in one
 * owner ({@link PageSize}) instead of being re-implemented per use case.
 */
class SharedSizeResolutionTest {

  private static final SortProductsRequest REQUEST =
    new SortProductsRequest(Map.of(Metrics.SALES_UNITS.key(), 1.0, Metrics.STOCK.key(), 0.0));

  private final ProductRepository repository = new StubRepository();

  @Test
  void shouldResolveTheSameSizeForListAndSort() {
    final var list = new ListProductsUseCase(repository).execute(null, 50);
    final var sort = new SortProductsUseCase(repository).execute(REQUEST, null, 50);

    assertThat(list.size()).as("list effective size").isEqualTo(50);
    assertThat(sort.size()).as("sort effective size").isEqualTo(50);
  }

  @Test
  void shouldDefaultTheSameWayForListAndSort() {
    final var list = new ListProductsUseCase(repository).execute(null, null);
    final var sort = new SortProductsUseCase(repository).execute(REQUEST, null, null);

    assertThat(list.size()).as("list default size").isEqualTo(PageSize.DEFAULT);
    assertThat(sort.size()).as("sort default size").isEqualTo(PageSize.DEFAULT);
  }

  @Test
  void shouldRejectTheSameOutOfRangeSizeForListAndSort() {
    assertThatThrownBy(() -> new ListProductsUseCase(repository).execute(null, 0))
      .as("list rejects a size below the minimum").isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new SortProductsUseCase(repository).execute(REQUEST, null, 101))
      .as("sort rejects a size above the maximum").isInstanceOf(IllegalArgumentException.class);
  }

  private static final class StubRepository implements ProductRepository {
    @Override
    public ProductPage findPage(final String cursor, final int limit) {
      return new ProductPage(List.of());
    }

    @Override
    public ProductPage sortByWeights(final AppliedWeights weights, final String cursor, final int limit) {
      return new ProductPage(List.of());
    }
  }
}
