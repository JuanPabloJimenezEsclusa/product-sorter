package dev.jpje.productsorter.adapter.rest.v1;

import dev.jpje.productsorter.api.v1.dto.ProductPageResponse;
import dev.jpje.productsorter.api.v1.dto.ProductResponse;
import dev.jpje.productsorter.api.v1.dto.StockDto;
import dev.jpje.productsorter.domain.model.Product;
import dev.jpje.productsorter.domain.port.ProductRepository.PagedResult;
import org.springframework.stereotype.Component;

@Component
public class ProductControllerMapper {

  private static final int DEFAULT_SIZE = 20;
  private static final int MAX_SIZE = 100;

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

  public ProductPageResponse toProductPage(final PagedResult result, final int size) {
    final var productPage = new ProductPageResponse();
    productPage.setData(result.products().stream()
      .map(ProductControllerMapper::toProductResponse)
      .toList());
    productPage.setTotal(result.total());
    productPage.setSize(size);
    productPage.setNextCursor(result.nextCursor());
    return productPage;
  }

  public int sizeOrDefault(final Integer size) {
    if (size == null || size < 1) {
      return DEFAULT_SIZE;
    }
    return Math.min(size, MAX_SIZE);
  }
}
