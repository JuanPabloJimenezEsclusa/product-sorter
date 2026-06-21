package com.acidtango.productsorter.domain.port;

import java.util.List;

import com.acidtango.productsorter.domain.model.Product;

public interface ProductRepository {
  List<Product> findAll();
}
