package com.acidtango.productsorter.adapter.rest.exception;

import com.acidtango.productsorter.api.v1.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ProductSorterException.class)
  public ResponseEntity<ErrorResponse> handleProductSorter(final ProductSorterException ex) {
    return ResponseEntity.status(ex.getStatus()).body(build(ex.getStatus(), ex.getCode(), ex.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(final IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(build(400, "BAD_REQUEST", ex.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneral(final Exception ex) {
    log.error("Unhandled exception", ex);
    return ResponseEntity.internalServerError().body(build(500, "INTERNAL_ERROR", "An unexpected error occurred"));
  }

  private static ErrorResponse build(final int status, final String code, final String message) {
    return new ErrorResponse()
      .status(status)
      .code(code)
      .message(message)
      .timestamp(OffsetDateTime.now());
  }
}
