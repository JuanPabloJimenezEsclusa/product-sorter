package com.acidtango.productsorter.domain.port;

import com.acidtango.productsorter.domain.model.Product;

import java.util.List;

public interface ProductRepository {
  List<Product> findAll();
}
