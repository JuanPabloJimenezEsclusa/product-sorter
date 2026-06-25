package dev.jpje.productsorter.adapter.rest.v1;

import dev.jpje.productsorter.api.v1.ProductsApi;
import dev.jpje.productsorter.api.v1.dto.ProductPageResponse;
import dev.jpje.productsorter.api.v1.dto.SortRequest;
import dev.jpje.productsorter.application.port.ListProducts;
import dev.jpje.productsorter.application.port.SortProducts;
import dev.jpje.productsorter.application.port.SortProductsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController implements ProductsApi {

  private static final Logger log = LoggerFactory.getLogger(ProductController.class);

  private final SortProducts sortProducts;
  private final ListProducts listProducts;

  public ProductController(final SortProducts sortProducts,
                           final ListProducts listProducts) {
    this.sortProducts = sortProducts;
    this.listProducts = listProducts;
  }

  @Override
  @PreAuthorize("hasAnyRole('operator')")
  public ResponseEntity<ProductPageResponse> sortProducts(final SortRequest sortRequest,
                                                          final String cursor,
                                                          final Integer size) {
    final var request = new SortProductsRequest(sortRequest.getWeights());
    final var resolvedSize = ProductControllerMapper.sizeOrDefault(size);
    final var result = sortProducts.execute(request, cursor, resolvedSize);

    log.debug("sort resp: count={}, hasMore={}", result.products().size(), result.hasMore());
    return ResponseEntity.ok(ProductControllerMapper.toProductPage(result, resolvedSize));
  }

  @Override
  @PreAuthorize("hasAnyRole('admin', 'operator')")
  public ResponseEntity<ProductPageResponse> getProducts(final String cursor, final Integer size) {
    final var resolvedSize = ProductControllerMapper.sizeOrDefault(size);
    final var result = listProducts.execute(cursor, resolvedSize);

    log.debug("get resp: count={}, hasMore={}", result.products().size(), result.hasMore());
    return ResponseEntity.ok(ProductControllerMapper.toProductPage(result, resolvedSize));
  }
}
