package dev.jpje.productsorter.adapter.rest.v1;

import dev.jpje.productsorter.api.v1.ProductsApi;
import dev.jpje.productsorter.api.v1.dto.ProductPageResponse;
import dev.jpje.productsorter.api.v1.dto.SortRequest;
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
  public ResponseEntity<ProductPageResponse> sortProducts(final SortRequest sortRequest,
                                                   final String cursor,
                                                   final Integer size) {
    final var request = new SortProductsRequest(sortRequest.getWeights());
    final var resolvedSize = mapper.sizeOrDefault(size);
    final var result = sortUseCase.execute(request, cursor, resolvedSize);
    return ResponseEntity.ok(mapper.toProductPage(result, resolvedSize));
  }

  @Override
  public ResponseEntity<ProductPageResponse> getProducts(final String cursor, final Integer size) {
    final var resolvedSize = mapper.sizeOrDefault(size);
    final var result = listUseCase.execute(cursor, resolvedSize);
    return ResponseEntity.ok(mapper.toProductPage(result, resolvedSize));
  }
}
