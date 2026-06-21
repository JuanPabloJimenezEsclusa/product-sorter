package com.acidtango.productsorter.application.service;

import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.acidtango.productsorter.domain.model.Product;
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
  public List<ScoredProduct> execute(final Map<String, Double> weights, final int page, final int size) {
    final var maxSales = repository.findMaxSalesUnits().orElse(1);
    final var scoreables = repository.findAllScoreable();
    final var results = sortingEngine.sortScoreables(scoreables, weights, maxSales);

    final var skip = (long) (page - 1) * size;
    final var pagedResults = results.stream().skip(skip).limit(size).toList();

    final var ids = pagedResults.stream().map(item -> item.product().productId()).toList();
    final var fullProducts = repository.findByIds(ids);
    final var productById = fullProducts.stream()
      .collect(toMap(Product::productId, Function.identity()));

    return pagedResults.stream()
      .map(item -> new ScoredProduct(productById.get(item.product().productId()), item.score()))
      .toList();
  }
}
