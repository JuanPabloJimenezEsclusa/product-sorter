package dev.jpje.productsorter.adapter.rest.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitInterceptorTest {

  private static final int SINGLE_PERMIT = 1;

  private RateLimitInterceptor interceptorAllowing() {
    final var limiter = RateLimiter.of("rest", RateLimiterConfig.custom()
      .limitForPeriod(RateLimitInterceptorTest.SINGLE_PERMIT)
      .limitRefreshPeriod(Duration.ofMinutes(1))
      .timeoutDuration(Duration.ZERO)
      .build());
    return new RateLimitInterceptor(limiter);
  }

  @Test
  void shouldAllowWhenPermitAvailable() {
    final var interceptor = interceptorAllowing();
    final var allowed = interceptor.preHandle(
      new MockHttpServletRequest(), new MockHttpServletResponse(), new Object());

    assertThat(allowed).as("request within the limit is allowed").isTrue();
  }

  @Test
  void shouldRejectWhenLimitExceeded() {
    final var interceptor = interceptorAllowing();
    final var request = new MockHttpServletRequest();
    final var response = new MockHttpServletResponse();
    final var handler = new Object();
    interceptor.preHandle(request, response, handler);

    assertThatThrownBy(() -> interceptor.preHandle(request, response, handler))
      .as("request over the limit is rejected")
      .isInstanceOf(RequestNotPermitted.class);
  }
}
