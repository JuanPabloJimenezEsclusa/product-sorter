package com.acidtango.productsorter.application.service;

import java.util.List;
import java.util.Map;

import com.acidtango.productsorter.domain.port.ProductRepository;
import com.acidtango.productsorter.domain.port.SortProductsUseCase;
import com.acidtango.productsorter.domain.service.ScoredProduct;
import com.acidtango.productsorter.domain.service.SortingEngine;

public class SortProductsUseCaseImpl implements SortProductsUseCase {

  private final ProductRepository repository;
  private final SortingEngine sortingEngine;

  public SortProductsUseCaseImpl(final ProductRepository repository, final SortingEngine sortingEngine) {
    this.repository = repository;
    this.sortingEngine = sortingEngine;
  }

  @Override
  public List<ScoredProduct> execute(final Map<String, Double> weights) {
    final var products = repository.findAll();
    return sortingEngine.sort(products, weights);
  }
}
