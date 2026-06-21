package com.acidtango.productsorter.adapter.rest.v1;

import com.acidtango.productsorter.api.v1.ProductsApi;
import com.acidtango.productsorter.api.v1.dto.SortRequest;
import com.acidtango.productsorter.api.v1.dto.SortResponse;
import com.acidtango.productsorter.domain.port.ListProductsUseCase;
import com.acidtango.productsorter.domain.port.SortProductsRequest;
import com.acidtango.productsorter.domain.port.SortProductsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController implements ProductsApi {

  private final SortProductsUseCase sortUseCase;
  private final ListProductsUseCase listUseCase;
  private final ProductControllerMapper mapper;

  public ProductController(final SortProductsUseCase sortUseCase,
                           final ListProductsUseCase listUseCase,
                           final ProductControllerMapper mapper) {
    this.sortUseCase = sortUseCase;
    this.listUseCase = listUseCase;
    this.mapper = mapper;
  }

  @Override
  public ResponseEntity<SortResponse> sortProducts(final SortRequest sortRequest,
                                                   final Integer page,
                                                   final Integer size) {
    final var weights = sortRequest.getWeights();
    final var request = new SortProductsRequest(weights);
    final var resolvedPage = mapper.pageOrDefault(page);
    final var resolvedSize = mapper.sizeOrDefault(size);
    final var scoredProducts = sortUseCase.execute(request.weights(), resolvedPage, resolvedSize);
    return ResponseEntity.ok(mapper.toSortResponse(scoredProducts, resolvedPage, resolvedSize));
  }

  @Override
  public ResponseEntity<com.acidtango.productsorter.api.v1.dto.ProductPage> getProducts(final Integer page, final Integer size) {
    final var resolvedPage = mapper.pageOrDefault(page);
    final var resolvedSize = mapper.sizeOrDefault(size);
    final var products = listUseCase.execute(resolvedPage, resolvedSize);
    return ResponseEntity.ok(mapper.toProductPage(products, resolvedPage, resolvedSize));
  }
}
