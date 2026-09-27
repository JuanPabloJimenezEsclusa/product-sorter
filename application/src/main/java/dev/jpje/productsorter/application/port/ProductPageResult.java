package dev.jpje.productsorter.application.port;

import java.util.List;

import dev.jpje.productsorter.domain.model.Product;

/**
 * Application-facing page result: the visible rows, the opaque continuation token the client echoes
 * back, and the effective page size. This is the single owner of the client-facing cursor, replacing
 * the previous shared adapter/application DTO whose token no consumer owned.
 */
public record ProductPageResult(List<Product> products, String nextCursor, int size) {
  public ProductPageResult {
    products = List.copyOf(products);
  }

  public boolean hasMore() {
    return nextCursor != null;
  }
}
