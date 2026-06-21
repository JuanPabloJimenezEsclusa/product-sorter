package dev.jpje.productsorter.application.port;

import java.util.List;

import dev.jpje.productsorter.domain.service.ScoredProduct;

public interface SortProductsUseCase {
  List<ScoredProduct> execute(SortProductsRequest request, int page, int size);
}
