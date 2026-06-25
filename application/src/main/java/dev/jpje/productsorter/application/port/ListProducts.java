package dev.jpje.productsorter.application.port;

import dev.jpje.productsorter.domain.port.ProductRepository.PagedResult;

public interface ListProducts {
  PagedResult execute(String cursor, Integer size);
}
