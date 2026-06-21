package dev.jpje.productsorter.application.service;

import java.util.List;

import dev.jpje.productsorter.application.port.ListProductsUseCase;
import dev.jpje.productsorter.domain.model.Product;
import dev.jpje.productsorter.domain.port.ProductRepository;

public class ListProductsUseCaseImpl implements ListProductsUseCase {

  private final ProductRepository repository;

  public ListProductsUseCaseImpl(final ProductRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<Product> execute(final int page, final int size) {
    return repository.findPage(page, size);
  }
}
