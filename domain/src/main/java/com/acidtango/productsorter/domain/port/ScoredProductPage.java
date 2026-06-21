package com.acidtango.productsorter.domain.port;

import com.acidtango.productsorter.domain.service.ScoredProduct;

import java.util.List;

public record ScoredProductPage(List<ScoredProduct> products, int page, int size, long total, int totalPages) {

  public static ScoredProductPage of(final List<ScoredProduct> products, final int page, final int size, final long total) {
    final var totalPages = (int) Math.ceil((double) total / size);
    return new ScoredProductPage(products, page, size, total, totalPages);
  }
}
