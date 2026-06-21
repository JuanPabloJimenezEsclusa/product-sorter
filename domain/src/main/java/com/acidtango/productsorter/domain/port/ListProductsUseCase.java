package com.acidtango.productsorter.domain.port;

public interface ListProductsUseCase {
  ProductPage execute(int page, int size);
}
