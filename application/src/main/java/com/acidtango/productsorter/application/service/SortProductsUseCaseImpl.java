package com.acidtango.productsorter.application.service;

import java.util.Map;

import com.acidtango.productsorter.domain.port.ProductRepository;
import com.acidtango.productsorter.domain.port.SortProductsUseCase;
import com.acidtango.productsorter.domain.service.ScoredProductPage;
import com.acidtango.productsorter.domain.service.SortingEngine;

public class SortProductsUseCaseImpl implements SortProductsUseCase {

  private final ProductRepository repository;
  private final SortingEngine sortingEngine;

  public SortProductsUseCaseImpl(final ProductRepository repository, final SortingEngine sortingEngine) {
    this.repository = repository;
    this.sortingEngine = sortingEngine;
  }

  @Override
  public ScoredProductPage execute(final Map<String, Double> weights, final int page, final int size) {
    final var products = repository.findAll();
    final var scored = sortingEngine.sort(products, weights);
    final var total = (long) scored.size();
    final var skip = (long) (page - 1) * size;
    final var paged = scored.stream().skip(skip).limit(size).toList();
    return ScoredProductPage.of(paged, page, size, total);
  }
}
