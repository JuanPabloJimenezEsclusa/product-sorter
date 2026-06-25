package dev.jpje.productsorter.adapter.rest.resilience;

import java.time.Duration;

import io.github.resilience4j.micrometer.tagged.TaggedRateLimiterMetrics;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RestResilienceConfig {

  @Bean
  public RateLimiter restRateLimiter(
      final MeterRegistry meterRegistry,
      @Value("${resilience.ratelimiter.limit-for-period:200}") final int limitForPeriod,
      @Value("${resilience.ratelimiter.refresh-period-seconds:1}") final long refreshPeriodSeconds) {
    final var config = RateLimiterConfig.custom()
      .limitForPeriod(limitForPeriod)
      .limitRefreshPeriod(Duration.ofSeconds(refreshPeriodSeconds))
      .timeoutDuration(Duration.ZERO)
      .build();
    final var registry = RateLimiterRegistry.of(config);
    TaggedRateLimiterMetrics.ofRateLimiterRegistry(registry).bindTo(meterRegistry);
    return registry.rateLimiter("rest");
  }

  @Bean
  public WebMvcConfigurer rateLimitWebMvcConfigurer(final RateLimiter restRateLimiter) {
    return new WebMvcConfigurer() {
      @Override
      public void addInterceptors(final @NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor(restRateLimiter)).addPathPatterns("/api/**");
      }
    };
  }
}
