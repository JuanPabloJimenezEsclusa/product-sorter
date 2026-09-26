package dev.jpje.productsorter.application.usecase;

import dev.jpje.productsorter.application.port.ListProducts;
import dev.jpje.productsorter.application.port.PageSize;
import dev.jpje.productsorter.application.port.ProductPageResult;
import dev.jpje.productsorter.domain.port.ProductRepository;

public class ListProductsUseCase implements ListProducts {

  private final ProductRepository repository;

  public ListProductsUseCase(final ProductRepository repository) {
    this.repository = repository;
  }

  @Override
  public ProductPageResult execute(final String cursor, final Integer size) {
    final var resolvedSize = PageSize.resolve(size);
    final var page = repository.findPage(cursor, resolvedSize + 1);
    return PageAssembler.assemble(page.products(), resolvedSize, _ -> 0);
  }
}
