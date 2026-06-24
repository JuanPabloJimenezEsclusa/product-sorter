package dev.jpje.productsorter.application.port;

import java.util.List;

import dev.jpje.productsorter.domain.model.Product;

public interface ListProductsUseCase {
  List<Product> execute(int page, int size);
}
