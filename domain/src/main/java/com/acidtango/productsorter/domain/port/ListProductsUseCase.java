package com.acidtango.productsorter.domain.port;

import java.util.List;

import com.acidtango.productsorter.domain.model.Product;

public interface ListProductsUseCase {
  List<Product> execute(int page, int size);
}
