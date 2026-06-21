package com.acidtango.productsorter.adapter.rest.v1;

import java.util.List;

import com.acidtango.productsorter.api.v1.dto.ProductResponse;
import com.acidtango.productsorter.api.v1.dto.ScoredProduct;
import com.acidtango.productsorter.api.v1.dto.SortResponse;
import com.acidtango.productsorter.api.v1.dto.StockEntry;
import org.springframework.stereotype.Component;

@Component
public class ProductControllerMapper {

  private static final int DEFAULT_PAGE = 1;
  private static final int DEFAULT_SIZE = 20;

  private static ProductResponse toProductResponse(final com.acidtango.productsorter.domain.model.Product product) {
    final var dto = new ProductResponse();
    dto.setId(product.productId().value());
    dto.setName(product.productName().value());
    dto.setSalesUnits(product.salesUnits().value());
    dto.setStock(product.stock().entries().stream()
      .map(stockEntry -> {
        final var entry = new StockEntry();
        entry.setSize(stockEntry.size().name());
        entry.setQuantity(stockEntry.quantity());
        return entry;
      })
      .toList());
    return dto;
  }

  public SortResponse toSortResponse(final List<com.acidtango.productsorter.domain.service.ScoredProduct> scoredProducts, final int page, final int size) {
    final var response = new SortResponse();
    response.setData(scoredProducts.stream()
      .map(scored -> {
        final var scoredDto = new ScoredProduct();
        scoredDto.setProduct(toProductResponse(scored.product()));
        scoredDto.setScore(scored.score());
        return scoredDto;
      })
      .toList());
    response.setPage(page);
    response.setSize(size);
    return response;
  }

  public com.acidtango.productsorter.api.v1.dto.ProductPage toProductPage(
      final List<com.acidtango.productsorter.domain.model.Product> products,
      final int page, final int size) {
    final var productPage = new com.acidtango.productsorter.api.v1.dto.ProductPage();
    productPage.setData(products.stream()
      .map(ProductControllerMapper::toProductResponse)
      .toList());
    productPage.setPage(page);
    productPage.setSize(size);
    return productPage;
  }

  public int pageOrDefault(final Integer page) {
    return page != null ? page : DEFAULT_PAGE;
  }

  public int sizeOrDefault(final Integer size) {
    return size != null ? size : DEFAULT_SIZE;
  }
}
