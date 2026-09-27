package dev.jpje.productsorter.adapter.rest.v1;

import dev.jpje.productsorter.api.v1.dto.ProductPageResponse;
import dev.jpje.productsorter.api.v1.dto.ProductResponse;
import dev.jpje.productsorter.api.v1.dto.StockDto;
import dev.jpje.productsorter.application.port.ProductPageResult;
import dev.jpje.productsorter.domain.model.Product;

final class ProductControllerMapper {

  private ProductControllerMapper() {
  }

  static ProductPageResponse toProductPage(final ProductPageResult result) {
    final var productPage = new ProductPageResponse();
    productPage.setData(result.products().stream()
      .map(ProductControllerMapper::toProductResponse)
      .toList());
    productPage.setSize(result.size());
    productPage.setNextCursor(result.nextCursor());
    return productPage;
  }

  private static ProductResponse toProductResponse(final Product product) {
    final var dto = new ProductResponse();
    dto.setId(product.productId().value());
    dto.setName(product.productName().value());
    dto.setSalesUnits(product.salesUnits().value());
    dto.setStock(product.stock().entries().stream()
      .map(stockEntry -> {
        final var entry = new StockDto();
        entry.setSize(stockEntry.size().name());
        entry.setQuantity(stockEntry.quantity());
        return entry;
      })
      .toList());
    dto.setScore(product.weightedScore());
    return dto;
  }
}
