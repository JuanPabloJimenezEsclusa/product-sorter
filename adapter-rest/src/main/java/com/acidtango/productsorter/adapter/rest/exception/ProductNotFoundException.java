package com.acidtango.productsorter.adapter.rest.exception;

public class ProductNotFoundException extends ProductSorterException {

  public ProductNotFoundException(final Long id) {
    super("PRODUCT_NOT_FOUND", 404, "Product not found: " + id);
  }
}
