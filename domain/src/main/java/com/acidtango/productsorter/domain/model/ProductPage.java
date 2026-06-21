package com.acidtango.productsorter.domain.model;

import java.util.List;

public record ProductPage(List<Product> products, int page, int size, long total, int totalPages) {

  public static ProductPage of(final List<Product> products, final int page, final int size, final long total) {
    final var totalPages = (int) Math.ceil((double) total / size);
    return new ProductPage(products, page, size, total, totalPages);
  }
}
