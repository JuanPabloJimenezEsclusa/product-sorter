package dev.jpje.productsorter.domain.port;

import java.io.Serializable;
import java.util.List;

import dev.jpje.productsorter.domain.model.AppliedWeights;
import dev.jpje.productsorter.domain.model.Product;

public interface ProductRepository {

  PagedResult findPage(String cursor, int limit);

  PagedResult sortByWeights(AppliedWeights weights, String cursor, int limit);

  record PagedResult(List<Product> products, int total, String nextCursor) implements Serializable {
    public PagedResult {
      products = List.copyOf(products);
    }

    public boolean hasMore() {
      return nextCursor != null;
    }
  }
}

