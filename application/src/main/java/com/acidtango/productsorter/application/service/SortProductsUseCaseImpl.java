package com.acidtango.productsorter.application.service;

import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.function.Function;

import com.acidtango.productsorter.domain.model.Product;
import com.acidtango.productsorter.domain.port.ProductRepository;
import com.acidtango.productsorter.domain.port.SortProductsUseCase;
import com.acidtango.productsorter.domain.service.ScoredProduct;
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
    final var maxSales = repository.findMaxSalesUnits().orElse(1);
    final var scoreables = repository.findAllScoreable();
    final var scored = sortingEngine.sortScoreables(scoreables, weights, maxSales);

    final var total = (long) scored.size();
    final var skip = (long) (page - 1) * size;
    final var paged = scored.stream().skip(skip).limit(size).toList();

    final var ids = paged.stream().map(s -> s.product().productId()).toList();
    final var fullProducts = repository.findByIds(ids);
    final var productById = fullProducts.stream()
      .collect(toMap(Product::productId, Function.identity()));

    final var result = paged.stream()
      .map(s -> new ScoredProduct(productById.get(s.product().productId()), s.score()))
      .toList();

    return ScoredProductPage.of(result, page, size, total);
  }
}
