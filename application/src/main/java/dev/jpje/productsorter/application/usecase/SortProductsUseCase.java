package dev.jpje.productsorter.application.usecase;

import dev.jpje.productsorter.application.port.PageSize;
import dev.jpje.productsorter.application.port.ProductPageResult;
import dev.jpje.productsorter.application.port.SortProducts;
import dev.jpje.productsorter.application.port.SortProductsRequest;
import dev.jpje.productsorter.domain.model.AppliedWeights;
import dev.jpje.productsorter.domain.port.ProductRepository;

public class SortProductsUseCase implements SortProducts {

  private final ProductRepository repository;

  public SortProductsUseCase(final ProductRepository repository) {
    this.repository = repository;
  }

  @Override
  public ProductPageResult execute(final SortProductsRequest request, final String cursor, final Integer size) {
    final var resolvedSize = PageSize.resolve(size);
    final var weights = AppliedWeights.fromMap(request.weights());
    final var page = repository.sortByWeights(weights, cursor, resolvedSize + 1);
    return PageAssembler.assemble(page.products(), resolvedSize,
      product -> product.weightedScore() == null ? 0 : product.weightedScore());
  }
}
