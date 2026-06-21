package com.acidtango.productsorter.adapter.rest.exception;

import java.time.Instant;

public record ErrorResponse(int status, String code, String message, Instant timestamp) {

  public static ErrorResponse of(final ProductSorterException ex) {
    return new ErrorResponse(ex.getStatus(), ex.getCode(), ex.getMessage(), Instant.now());
  }

  public static ErrorResponse of(final int status, final String code, final String message) {
    return new ErrorResponse(status, code, message, Instant.now());
  }
}
