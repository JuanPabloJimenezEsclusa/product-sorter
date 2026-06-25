package dev.jpje.productsorter.adapter.rest.exception;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import dev.jpje.productsorter.api.v1.dto.ErrorResponse;
import dev.jpje.productsorter.domain.exception.RepositoryUnavailableException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(final IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(build(400, "BAD_REQUEST", ex.getMessage()));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleMalformedBody(final HttpMessageNotReadableException ex) {
    return ResponseEntity.badRequest().body(build(400, "BAD_REQUEST", ex.getMessage()));
  }

  @ExceptionHandler(RequestNotPermitted.class)
  public ResponseEntity<ErrorResponse> handleRateLimited(final RequestNotPermitted ex) {
    log.warn("Rate limit exceeded: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
      .body(build(429, "TOO_MANY_REQUESTS", "Too many requests, please retry later"));
  }

  @ExceptionHandler(RepositoryUnavailableException.class)
  public ResponseEntity<ErrorResponse> handleUnavailable(final RepositoryUnavailableException ex) {
    log.error("Repository unavailable", ex);
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
      .body(build(503, "SERVICE_UNAVAILABLE", "Service temporarily unavailable, please retry later"));
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
      .timestamp(OffsetDateTime.now(ZoneId.systemDefault()));
  }
}
