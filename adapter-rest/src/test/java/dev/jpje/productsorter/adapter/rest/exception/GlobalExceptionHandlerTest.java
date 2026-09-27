package dev.jpje.productsorter.adapter.rest.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

class GlobalExceptionHandlerTest {

  @ParameterizedTest(name = "{0}")
  @MethodSource("exceptionMappings")
  void shouldMapExceptionToStatusAndCode(final Supplier<ResponseEntity<ErrorResponse>> handled,
                                         final HttpStatus expectedStatus,
                                         final String expectedCode) {
    final var response = handled.get();

    assertThat(response.getStatusCode().value())
      .as("HTTP status for %s", expectedCode).isEqualTo(expectedStatus.value());
    assertThat(response.getBody()).isNotNull()
      .extracting(errorResponse -> errorResponse != null ? errorResponse.getCode() : null)
      .as("error code for %s", expectedStatus).isEqualTo(expectedCode);
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

  private static Stream<Arguments> exceptionMappings() {
    final var h = new GlobalExceptionHandler();
    return Stream.of(
      arguments(named("IllegalArgument -> 400",
        (Supplier<ResponseEntity<ErrorResponse>>) () -> h.handleIllegalArgument(new IllegalArgumentException("bad"))),
        HttpStatus.BAD_REQUEST, "BAD_REQUEST"),
      arguments(named("RequestNotPermitted -> 429",
        (Supplier<ResponseEntity<ErrorResponse>>) () -> h.handleRateLimited(
          RequestNotPermitted.createRequestNotPermitted(RateLimiter.ofDefaults("rest")))),
        HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS"),
      arguments(named("RepositoryUnavailable -> 503",
        (Supplier<ResponseEntity<ErrorResponse>>) () -> h.handleUnavailable(
          new RepositoryUnavailableException("down", new RuntimeException()))),
        HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE"),
      arguments(named("generic Exception -> 500",
        (Supplier<ResponseEntity<ErrorResponse>>) () -> h.handleGeneral(new RuntimeException("boom"))),
        HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR"));
  }
}
