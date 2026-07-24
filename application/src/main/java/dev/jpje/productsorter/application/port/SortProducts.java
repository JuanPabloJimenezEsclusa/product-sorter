package dev.jpje.productsorter.application.port;

import dev.jpje.productsorter.domain.port.ProductRepository.PagedResult;

public interface SortProducts {
  PagedResult execute(SortProductsRequest request, String cursor, Integer size);
}
