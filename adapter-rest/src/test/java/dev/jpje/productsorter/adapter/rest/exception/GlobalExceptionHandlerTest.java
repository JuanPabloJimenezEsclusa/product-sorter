package dev.jpje.productsorter.adapter.rest.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import dev.jpje.productsorter.api.v1.dto.ErrorResponse;
import dev.jpje.productsorter.domain.exception.RepositoryUnavailableException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

  @ParameterizedTest(name = "{0}")
  @MethodSource("exceptionMappings")
  void shouldMapExceptionToStatusAndCode(final Supplier<ResponseEntity<ErrorResponse>> handled,
                                         final HttpStatus expectedStatus,
                                         final String expectedCode,
                                         final String expectedMessage,
                                         final String rawExceptionText) {
    final var response = handled.get();
    final var body = response.getBody();

    assertThat(response.getStatusCode().value())
      .as("HTTP status for %s", expectedCode).isEqualTo(expectedStatus.value());
    assertThat(body).as("error body for %s", expectedCode).isNotNull();
    assertThat(body.getCode())
      .as("error code for %s", expectedStatus).isEqualTo(expectedCode);
    assertThat(body.getMessage())
      .as("message for %s must be stable generic text and must not echo raw exception text", expectedStatus)
      .isEqualTo(expectedMessage)
      .doesNotContain(rawExceptionText);
  }

  @Test
  void shouldNotEchoRawExceptionTextInBadRequestMessages() {
    final var handler = new GlobalExceptionHandler();

    assertThat(handler.handleIllegalArgument(
      new IllegalArgumentException("Metrics.validateWeights: weight 2.0 out of range")).getBody())
      .extracting(ErrorResponse::getMessage)
      .as("a 400 message must be stable and must not echo the validation exception text")
      .isEqualTo("Invalid request");

    assertThat(handler.handleMalformedBody(
      new HttpMessageNotReadableException("PageSize.resolve: size out of range", null)).getBody())
      .extracting(ErrorResponse::getMessage)
      .as("a 400 message must be stable and must not echo the malformed-body exception text")
      .isEqualTo("Invalid request");
  }

  private static Stream<Arguments> exceptionMappings() throws NoSuchMethodException {
    final var h = new GlobalExceptionHandler();

    final var illegalArgument = new IllegalArgumentException("Metrics.validateWeights: weight 2.0 out of range");
    final var malformedBody = new HttpMessageNotReadableException("PageSize.resolve: size out of range", null);
    final var typeMismatch = new MethodArgumentTypeMismatchException("abc", Integer.class, "size", null, null);
    final var missingParameter = new MissingServletRequestParameterException("cursor", "String");
    final var argumentNotValid = new MethodArgumentNotValidException(
      validationParameter(), new BeanPropertyBindingResult(new Object(), "weights"));
    final var handlerMethodValidation =
      new HandlerMethodValidationException(mock(MethodValidationResult.class));
    final var constraintViolation = new ConstraintViolationException("size must be positive", Set.of());
    final var methodNotSupported = new HttpRequestMethodNotSupportedException("DELETE");
    final var mediaTypeNotSupported = new HttpMediaTypeNotSupportedException(
      MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON));
    final var noResourceFound = new NoResourceFoundException(HttpMethod.GET, "/api/v1/missing", "/api/v1/missing");
    final var rateLimited = RequestNotPermitted.createRequestNotPermitted(RateLimiter.ofDefaults("rest"));
    final var unavailable = new RepositoryUnavailableException("catalog store unreachable", new RuntimeException());
    final var unexpected = new RuntimeException("boom");

    return Stream.of(
      arguments(named("IllegalArgument -> 400",
        (Supplier<ResponseEntity<ErrorResponse>>) () -> h.handleIllegalArgument(illegalArgument)),
        HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid request", illegalArgument.getMessage()),
      arguments(named("MalformedBody -> 400",
        (Supplier<ResponseEntity<ErrorResponse>>) () -> h.handleMalformedBody(malformedBody)),
        HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid request", malformedBody.getMessage()),
      arguments(named("MethodArgumentTypeMismatch -> 400",
        (Supplier<ResponseEntity<ErrorResponse>>) () -> h.handleTypeMismatch(typeMismatch)),
        HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid request", typeMismatch.getMessage()),
      arguments(named("MissingServletRequestParameter -> 400",
        (Supplier<ResponseEntity<ErrorResponse>>) () -> h.handleMissingParameter(missingParameter)),
        HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid request", missingParameter.getMessage()),
      arguments(named("MethodArgumentNotValid -> 400",
        (Supplier<ResponseEntity<ErrorResponse>>) () -> h.handleArgumentNotValid(argumentNotValid)),
        HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid request", argumentNotValid.getMessage()),
      arguments(named("HandlerMethodValidation -> 400",
        (Supplier<ResponseEntity<ErrorResponse>>) () -> h.handleHandlerMethodValidation(handlerMethodValidation)),
        HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid request", handlerMethodValidation.getMessage()),
      arguments(named("ConstraintViolation -> 400",
        (Supplier<ResponseEntity<ErrorResponse>>) () -> h.handleConstraintViolation(constraintViolation)),
        HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid request", constraintViolation.getMessage()),
      arguments(named("MethodNotSupported -> 405",
        (Supplier<ResponseEntity<ErrorResponse>>) () -> h.handleMethodNotSupported(methodNotSupported)),
        HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Method not allowed",
        methodNotSupported.getMessage()),
      arguments(named("MediaTypeNotSupported -> 415",
        (Supplier<ResponseEntity<ErrorResponse>>) () -> h.handleMediaTypeNotSupported(mediaTypeNotSupported)),
        HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "Unsupported media type",
        mediaTypeNotSupported.getMessage()),
      arguments(named("NoResourceFound -> 404",
        (Supplier<ResponseEntity<ErrorResponse>>) () -> h.handleNoResourceFound(noResourceFound)),
        HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found", noResourceFound.getMessage()),
      arguments(named("RequestNotPermitted -> 429",
        (Supplier<ResponseEntity<ErrorResponse>>) () -> h.handleRateLimited(rateLimited)),
        HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", "Too many requests, please retry later",
        rateLimited.getMessage()),
      arguments(named("RepositoryUnavailable -> 503",
        (Supplier<ResponseEntity<ErrorResponse>>) () -> h.handleUnavailable(unavailable)),
        HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", "Service temporarily unavailable, please retry later",
        unavailable.getMessage()),
      arguments(named("generic Exception -> 500",
        (Supplier<ResponseEntity<ErrorResponse>>) () -> h.handleGeneral(unexpected)),
        HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred",
        unexpected.getMessage()));
  }

  /**
   * Fixture holder: only its parameter is read, to build the {@link MethodParameter} required by
   * {@link MethodArgumentNotValidException}.
   */
  private static void validationTarget(final String value) {
    // Intentionally empty: the method exists only to be referenced reflectively.
  }

  private static MethodParameter validationParameter() throws NoSuchMethodException {
    return new MethodParameter(
      GlobalExceptionHandlerTest.class.getDeclaredMethod("validationTarget", String.class), 0);
  }
}
