package com.acidtango.productsorter.domain.port;

import java.util.List;
import java.util.OptionalInt;

import com.acidtango.productsorter.domain.model.Product;
import com.acidtango.productsorter.domain.model.ScoreableProduct;
import com.acidtango.productsorter.domain.vo.ProductId;

public interface ProductRepository {
  List<Product> findPage(int page, int size);
  OptionalInt findMaxSalesUnits();
  List<ScoreableProduct> findAllScoreable();
  List<Product> findByIds(List<ProductId> ids);
}
