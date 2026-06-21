package com.acidtango.productsorter.adapter.rest.v1;

import com.acidtango.productsorter.api.v1.dto.*;
import com.acidtango.productsorter.domain.service.ScoredProductPage;
import org.springframework.stereotype.Component;

@Component
public class ProductControllerMapper {

  private static final int DEFAULT_PAGE = 1;
  private static final int DEFAULT_SIZE = 20;

  public SortResponse toSortResponse(final ScoredProductPage domain) {
    final var response = new SortResponse();
    response.setData(domain.products().stream()
      .map(sp -> {
        final var dto = new ScoredProduct();
        dto.setId(sp.product().productId().value());
        dto.setName(sp.product().productName().value());
        dto.setScore(sp.score());
        return dto;
      })
      .toList());
    response.setPage(domain.page());
    response.setSize(domain.size());
    response.setTotal(domain.total());
    response.setTotalPages(domain.totalPages());
    return response;
  }

  public com.acidtango.productsorter.api.v1.dto.ProductPage toProductPage(
      final com.acidtango.productsorter.domain.model.ProductPage domain) {
    final var page = new com.acidtango.productsorter.api.v1.dto.ProductPage();
    page.setData(domain.products().stream()
      .map(p -> {
        final var dto = new ProductResponse();
        dto.setId(p.productId().value());
        dto.setName(p.productName().value());
        dto.setSalesUnits(p.salesUnits().value());
        dto.setStock(p.stock().entries().stream()
          .map(e -> {
            final var entry = new StockEntry();
            entry.setSize(e.size().name());
            entry.setQuantity(e.quantity());
            return entry;
          })
          .toList());
        return dto;
      })
      .toList());
    page.setPage(domain.page());
    page.setSize(domain.size());
    page.setTotal(domain.total());
    page.setTotalPages(domain.totalPages());
    return page;
  }

  public int pageOrDefault(final Integer page) {
    return page != null ? page : DEFAULT_PAGE;
  }

  public int sizeOrDefault(final Integer size) {
    return size != null ? size : DEFAULT_SIZE;
  }
}
