package dev.jpje.productsorter.application.port;

public interface SortProducts {
  ProductPageResult execute(SortProductsRequest request, String cursor, Integer size);
}
