package com.acidtango.productsorter.adapter.rest.v1;

import com.acidtango.productsorter.adapter.rest.exception.InvalidWeightsException;
import com.acidtango.productsorter.api.v1.ProductsApi;
import com.acidtango.productsorter.api.v1.dto.ScoredProduct;
import com.acidtango.productsorter.api.v1.dto.SortRequest;
import com.acidtango.productsorter.api.v1.dto.SortResponse;
import com.acidtango.productsorter.application.service.SortProductsRequest;
import com.acidtango.productsorter.domain.port.SortProductsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController implements ProductsApi {

  private final SortProductsUseCase useCase;

  public ProductController(final SortProductsUseCase useCase) {
    this.useCase = useCase;
  }

  @Override
  public ResponseEntity<SortResponse> sortProducts(final SortRequest sortRequest) {
    final var weights = sortRequest.getWeights();
    if (weights == null || weights.isEmpty()) {
      throw new InvalidWeightsException("Weights must not be null or empty");
    }
    final var request = new SortProductsRequest(weights);
    final var scoredProducts = useCase.execute(request.weights());
    final var response = new SortResponse();
    response.setSortedProducts(scoredProducts.stream()
      .map(sp -> {
        final var dto = new ScoredProduct();
        dto.setId(sp.product().productId().value());
        dto.setName(sp.product().productName().value());
        dto.setScore(sp.score());
        return dto;
      })
      .toList());
    return ResponseEntity.ok(response);
  }
}
