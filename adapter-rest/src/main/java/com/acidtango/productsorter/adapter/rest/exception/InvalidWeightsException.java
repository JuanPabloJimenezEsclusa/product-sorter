package com.acidtango.productsorter.adapter.rest.exception;

public class InvalidWeightsException extends ProductSorterException {

  public InvalidWeightsException(final String message) {
    super("INVALID_WEIGHTS", 400, message);
  }
}
