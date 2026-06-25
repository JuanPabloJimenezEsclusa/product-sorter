package dev.jpje.productsorter.adapter.rest.resilience;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.jspecify.annotations.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

public class RateLimitInterceptor implements HandlerInterceptor {

  private final RateLimiter rateLimiter;

  public RateLimitInterceptor(final RateLimiter rateLimiter) {
    this.rateLimiter = rateLimiter;
  }

  @Override
  public boolean preHandle(final @NonNull HttpServletRequest request,
                           final @NonNull HttpServletResponse response,
                           final @NonNull Object handler) {
    if (!rateLimiter.acquirePermission()) {
      throw RequestNotPermitted.createRequestNotPermitted(rateLimiter);
    }
    return true;
  }
}
