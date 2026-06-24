package dev.jpje.productsorter.domain.port;

import java.util.List;
import java.util.OptionalInt;

import dev.jpje.productsorter.domain.model.Product;
import dev.jpje.productsorter.domain.model.ScoreableProduct;
import dev.jpje.productsorter.domain.vo.ProductId;

public interface ProductRepository {
  List<Product> findPage(int page, int size);
  OptionalInt findMaxSalesUnits();
  List<ScoreableProduct> findAllScoreable();
  List<Product> findByIds(List<ProductId> ids);
}
