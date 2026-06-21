package com.acidtango.productsorter.domain.port;

import com.acidtango.productsorter.domain.model.ProductPage;

public interface ListProductsUseCase {
  ProductPage execute(int page, int size);
}
