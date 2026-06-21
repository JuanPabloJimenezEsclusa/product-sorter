package com.acidtango.productsorter.adapter.rest.exception;

public abstract class ProductSorterException extends RuntimeException {

  private final String code;
  private final int status;

  protected ProductSorterException(final String code, final int status, final String message) {
    super(message);
    this.code = code;
    this.status = status;
  }

  public String getCode() {
    return code;
  }

  public int getStatus() {
    return status;
  }
}
