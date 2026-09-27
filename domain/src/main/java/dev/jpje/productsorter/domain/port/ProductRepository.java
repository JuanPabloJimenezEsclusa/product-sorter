package dev.jpje.productsorter.domain.port;

import dev.jpje.productsorter.domain.model.AppliedWeights;

public interface ProductRepository {

  ProductPage findPage(String cursor, int limit);

  ProductPage sortByWeights(AppliedWeights weights, String cursor, int limit);
}
