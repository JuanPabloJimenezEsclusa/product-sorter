package com.acidtango.productsorter.adapter.rest.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ProductSorterException.class)
  public ResponseEntity<ErrorResponse> handleProductSorter(final ProductSorterException ex) {
    return ResponseEntity.status(ex.getStatus()).body(ErrorResponse.of(ex));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(final IllegalArgumentException ex) {
    return ResponseEntity.badRequest()
      .body(ErrorResponse.of(400, "BAD_REQUEST", ex.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneral(final Exception ex) {
    log.error("Unhandled exception", ex);
    return ResponseEntity.internalServerError()
      .body(ErrorResponse.of(500, "INTERNAL_ERROR", "An unexpected error occurred"));
  }
}
