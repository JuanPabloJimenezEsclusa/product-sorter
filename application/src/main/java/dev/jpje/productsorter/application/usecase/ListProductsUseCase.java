package dev.jpje.productsorter.application.usecase;

import java.util.List;

import dev.jpje.productsorter.application.port.ListProducts;
import dev.jpje.productsorter.domain.port.ProductRepository;
import dev.jpje.productsorter.domain.port.ProductRepository.PagedResult;
import dev.jpje.productsorter.domain.vo.CursorCodec;

public class ListProductsUseCase implements ListProducts {

  private final ProductRepository repository;

  public ListProductsUseCase(final ProductRepository repository) {
    this.repository = repository;
  }

  @Override
  public PagedResult execute(final String cursor, final Integer size) {
    if (size == null || size < 1 || size == Integer.MAX_VALUE) {
      return new PagedResult(List.of(), null);
    }
    final var limit = size + 1;
    final var page = repository.findPage(cursor, limit);
    final var hasMore = page.products().size() > size;
    final var trimmed = hasMore ? page.products().subList(0, size) : page.products();

    final var nextCursor = hasMore && !trimmed.isEmpty()
      ? CursorCodec.encode(0, trimmed.getLast().productId().value())
      : null;

    return new PagedResult(trimmed, nextCursor);
  }
}
