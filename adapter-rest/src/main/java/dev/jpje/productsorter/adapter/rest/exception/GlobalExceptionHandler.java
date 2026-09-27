package dev.jpje.productsorter.adapter.rest.exception;

import jakarta.validation.ConstraintViolationException;
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
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String INVALID_REQUEST_MESSAGE = "Invalid request";

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(final IllegalArgumentException ex) {
    log.debug("Rejected invalid request argument: {}", ex.getMessage(), ex);
    return ResponseEntity.badRequest().body(build(400, "BAD_REQUEST", INVALID_REQUEST_MESSAGE));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleMalformedBody(final HttpMessageNotReadableException ex) {
    log.debug("Rejected unreadable request body: {}", ex.getMessage(), ex);
    return ResponseEntity.badRequest().body(build(400, "BAD_REQUEST", INVALID_REQUEST_MESSAGE));
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

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(final MethodArgumentTypeMismatchException ex) {
    log.debug("Rejected mistyped request parameter '{}': {}", ex.getName(), ex.getMessage(), ex);
    return ResponseEntity.badRequest().body(build(400, "BAD_REQUEST", INVALID_REQUEST_MESSAGE));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingParameter(final MissingServletRequestParameterException ex) {
    log.debug("Rejected request missing parameter '{}': {}", ex.getParameterName(), ex.getMessage(), ex);
    return ResponseEntity.badRequest().body(build(400, "BAD_REQUEST", INVALID_REQUEST_MESSAGE));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleArgumentNotValid(final MethodArgumentNotValidException ex) {
    log.debug("Rejected request failing argument validation: {}", ex.getMessage(), ex);
    return ResponseEntity.badRequest().body(build(400, "BAD_REQUEST", INVALID_REQUEST_MESSAGE));
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(final HandlerMethodValidationException ex) {
    log.debug("Rejected request failing handler method validation: {}", ex.getMessage(), ex);
    return ResponseEntity.badRequest().body(build(400, "BAD_REQUEST", INVALID_REQUEST_MESSAGE));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(final ConstraintViolationException ex) {
    log.debug("Rejected request violating constraints: {}", ex.getMessage(), ex);
    return ResponseEntity.badRequest().body(build(400, "BAD_REQUEST", INVALID_REQUEST_MESSAGE));
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupported(final HttpRequestMethodNotSupportedException ex) {
    log.debug("Rejected request using unsupported method '{}': {}", ex.getMethod(), ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
      .body(build(405, "METHOD_NOT_ALLOWED", "Method not allowed"));
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(final HttpMediaTypeNotSupportedException ex) {
    log.debug("Rejected request with unsupported media type: {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
      .body(build(415, "UNSUPPORTED_MEDIA_TYPE", "Unsupported media type"));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoResourceFound(final NoResourceFoundException ex) {
    log.debug("Rejected request for missing resource '{}': {}", ex.getResourcePath(), ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
      .body(build(404, "NOT_FOUND", "Resource not found"));
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
