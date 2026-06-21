package com.acidtango.productsorter.adapter.rest.v1;

import com.acidtango.productsorter.api.v1.ProductsApi;
import com.acidtango.productsorter.api.v1.dto.*;
import com.acidtango.productsorter.application.service.SortProductsRequest;
import com.acidtango.productsorter.domain.port.ListProductsUseCase;
import com.acidtango.productsorter.domain.port.ScoredProductPage;
import com.acidtango.productsorter.domain.port.SortProductsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController implements ProductsApi {

  private final SortProductsUseCase sortUseCase;
  private final ListProductsUseCase listUseCase;

  public ProductController(final SortProductsUseCase sortUseCase, final ListProductsUseCase listUseCase) {
    this.sortUseCase = sortUseCase;
    this.listUseCase = listUseCase;
  }

  @Override
  public ResponseEntity<SortResponse> sortProducts(final SortRequest sortRequest,
                                                     final Integer page,
                                                     final Integer size) {
    final var weights = sortRequest.getWeights();
    final var request = new SortProductsRequest(weights);
    final var result = sortUseCase.execute(request.weights(), pageOrDefault(page), sizeOrDefault(size));
    return ResponseEntity.ok(toSortResponse(result));
  }

  @Override
  public ResponseEntity<ProductPage> getProducts(final Integer page, final Integer size) {
    final var result = listUseCase.execute(pageOrDefault(page), sizeOrDefault(size));
    return ResponseEntity.ok(toProductPage(result));
  }

  private static int pageOrDefault(final Integer page) {
    return page != null ? page : 1;
  }

  private static int sizeOrDefault(final Integer size) {
    return size != null ? size : 20;
  }

  private static SortResponse toSortResponse(final ScoredProductPage domain) {
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

  private static ProductPage toProductPage(final com.acidtango.productsorter.domain.port.ProductPage domain) {
    final var page = new ProductPage();
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
}
