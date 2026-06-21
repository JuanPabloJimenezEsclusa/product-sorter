package dev.jpje.productsorter.application.service;

import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.function.Function;

import dev.jpje.productsorter.application.port.SortProductsRequest;
import dev.jpje.productsorter.application.port.SortProductsUseCase;
import dev.jpje.productsorter.domain.model.Product;
import dev.jpje.productsorter.domain.port.ProductRepository;
import dev.jpje.productsorter.domain.service.ScoredProduct;
import dev.jpje.productsorter.domain.service.SortingEngine;

public class SortProductsUseCaseImpl implements SortProductsUseCase {

  private final ProductRepository repository;
  private final SortingEngine sortingEngine;

  public SortProductsUseCaseImpl(final ProductRepository repository, final SortingEngine sortingEngine) {
    this.repository = repository;
    this.sortingEngine = sortingEngine;
  }

  @Override
  public List<ScoredProduct> execute(final SortProductsRequest request, final int page, final int size) {
    if (page < 1 || size < 1) {
      return List.of();
    }
    final var weights = request.weights();
    final var maxSales = repository.findMaxSalesUnits().orElse(1);
    final var scoreables = repository.findAllScoreable();
    final var results = sortingEngine.sortScoreables(scoreables, weights, maxSales);

    final var skip = ((long) page - 1) * size;
    final var pagedResults = results.stream().skip(skip).limit(size).toList();

    final var ids = pagedResults.stream().map(item -> item.productId()).toList();
    final var fullProducts = repository.findByIds(ids);
    final var productById = fullProducts.stream()
      .collect(toMap(Product::productId, Function.identity()));

    return pagedResults.stream()
      .map(item -> new ScoredProduct(productById.get(item.productId()), item.score()))
      .toList();
  }
}
