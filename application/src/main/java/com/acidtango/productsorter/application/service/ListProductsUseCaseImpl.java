package com.acidtango.productsorter.application.service;

import java.util.List;

import com.acidtango.productsorter.domain.model.Product;
import com.acidtango.productsorter.domain.port.ListProductsUseCase;
import com.acidtango.productsorter.domain.port.ProductRepository;

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
