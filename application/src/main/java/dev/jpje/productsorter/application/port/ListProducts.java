package dev.jpje.productsorter.application.port;

public interface ListProducts {
  ProductPageResult execute(String cursor, Integer size);
}
