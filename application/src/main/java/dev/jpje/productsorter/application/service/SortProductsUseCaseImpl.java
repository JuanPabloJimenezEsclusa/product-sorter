package dev.jpje.productsorter.application.service;

import java.util.List;

import dev.jpje.productsorter.application.port.SortProductsRequest;
import dev.jpje.productsorter.application.port.SortProductsUseCase;
import dev.jpje.productsorter.domain.model.AppliedWeights;
import dev.jpje.productsorter.domain.port.ProductRepository;
import dev.jpje.productsorter.domain.port.ProductRepository.PagedResult;
import dev.jpje.productsorter.domain.vo.CursorCodec;

public class SortProductsUseCaseImpl implements SortProductsUseCase {

  private final ProductRepository repository;

  public SortProductsUseCaseImpl(final ProductRepository repository) {
    this.repository = repository;
  }

  @Override
  public PagedResult execute(final SortProductsRequest request, final String cursor, final Integer size) {
    if (size == null || size < 1) {
      return new PagedResult(List.of(), null);
    }
    final var weights = AppliedWeights.fromMap(request.weights());
    final var limit = size + 1;
    final var page = repository.sortByWeights(weights, cursor, limit);
    final var hasMore = page.products().size() > size;
    final var trimmed = hasMore ? page.products().subList(0, size) : page.products();

    final var nextCursor = hasMore && !trimmed.isEmpty()
      ? CursorCodec.encode(
          trimmed.getLast().weightedScore(),
          trimmed.getLast().productId().value())
      : null;

    return new PagedResult(List.copyOf(trimmed), nextCursor);
  }
}
