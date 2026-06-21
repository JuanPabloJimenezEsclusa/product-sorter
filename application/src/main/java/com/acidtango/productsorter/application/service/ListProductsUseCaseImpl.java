package com.acidtango.productsorter.application.service;

import com.acidtango.productsorter.domain.port.ListProductsUseCase;
import com.acidtango.productsorter.domain.port.ProductPage;
import com.acidtango.productsorter.domain.port.ProductRepository;

public class ListProductsUseCaseImpl implements ListProductsUseCase {

  private final ProductRepository repository;

  public ListProductsUseCaseImpl(final ProductRepository repository) {
    this.repository = repository;
  }

  @Override
  public ProductPage execute(final int page, final int size) {
    final var all = repository.findAll();
    final var total = (long) all.size();
    final var skip = (long) (page - 1) * size;
    final var paged = all.stream().skip(skip).limit(size).toList();
    return ProductPage.of(paged, page, size, total);
  }
}
