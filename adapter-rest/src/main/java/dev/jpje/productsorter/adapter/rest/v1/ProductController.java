package dev.jpje.productsorter.adapter.rest.v1;

import dev.jpje.productsorter.api.v1.ProductsApi;
import dev.jpje.productsorter.api.v1.dto.SortRequest;
import dev.jpje.productsorter.api.v1.dto.SortResponse;
import dev.jpje.productsorter.application.port.ListProductsUseCase;
import dev.jpje.productsorter.application.port.SortProductsRequest;
import dev.jpje.productsorter.application.port.SortProductsUseCase;
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
    final var request = new SortProductsRequest(sortRequest.getWeights());
    final var resolvedPage = mapper.pageOrDefault(page);
    final var resolvedSize = mapper.sizeOrDefault(size);
    final var scoredProducts = sortUseCase.execute(request, resolvedPage, resolvedSize);
    return ResponseEntity.ok(mapper.toSortResponse(scoredProducts, resolvedPage, resolvedSize));
  }

  @Override
  public ResponseEntity<dev.jpje.productsorter.api.v1.dto.ProductPage> getProducts(final Integer page, final Integer size) {
    final var resolvedPage = mapper.pageOrDefault(page);
    final var resolvedSize = mapper.sizeOrDefault(size);
    final var products = listUseCase.execute(resolvedPage, resolvedSize);
    return ResponseEntity.ok(mapper.toProductPage(products, resolvedPage, resolvedSize));
  }
}
