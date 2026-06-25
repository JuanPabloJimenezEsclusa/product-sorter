package dev.jpje.productsorter.adapter.persistence.resilience;

import java.time.Duration;

import dev.jpje.productsorter.adapter.persistence.mongo.MongoProductRepositoryAdapter;
import dev.jpje.productsorter.domain.port.ProductRepository;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.micrometer.tagged.TaggedBulkheadMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class PersistenceResilienceConfig {

  private static final String RESILIENCE_LABEL = "mongo";

  @Bean
  public CircuitBreakerRegistry circuitBreakerRegistry(
      final MeterRegistry meterRegistry,
      @Value("${resilience.circuitbreaker.failure-rate-threshold:50}") final float failureRate,
      @Value("${resilience.circuitbreaker.slow-call-rate-threshold:100}") final float slowCallRate,
      @Value("${resilience.circuitbreaker.slow-call-duration-seconds:6}") final long slowCallSeconds,
      @Value("${resilience.circuitbreaker.sliding-window-size:30}") final int slidingWindow,
      @Value("${resilience.circuitbreaker.minimum-number-of-calls:50}") final int minimumCalls,
      @Value("${resilience.circuitbreaker.wait-open-seconds:10}") final long waitOpenSeconds) {
    final var config = CircuitBreakerConfig.custom()
      .failureRateThreshold(failureRate)
      .slowCallRateThreshold(slowCallRate)
      .slowCallDurationThreshold(Duration.ofSeconds(slowCallSeconds))
      .slidingWindowType(SlidingWindowType.TIME_BASED)
      .slidingWindowSize(slidingWindow)
      .minimumNumberOfCalls(minimumCalls)
      .waitDurationInOpenState(Duration.ofSeconds(waitOpenSeconds))
      .permittedNumberOfCallsInHalfOpenState(5)
      .build();
    final var registry = CircuitBreakerRegistry.of(config);
    TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry).bindTo(meterRegistry);
    return registry;
  }

  @Bean
  public RetryRegistry retryRegistry(
      final MeterRegistry meterRegistry,
      @Value("${resilience.retry.max-attempts:3}") final int maxAttempts,
      @Value("${resilience.retry.initial-backoff-millis:100}") final long initialBackoffMillis) {
    final var config = RetryConfig.custom()
      .maxAttempts(maxAttempts)
      .intervalFunction(IntervalFunction
        .ofExponentialRandomBackoff(Duration.ofMillis(initialBackoffMillis), 2.0, 0.5))
      .retryOnException(MongoFaultClassifier::isRetryable)
      .build();
    final var registry = RetryRegistry.of(config);
    TaggedRetryMetrics.ofRetryRegistry(registry).bindTo(meterRegistry);
    return registry;
  }

  @Bean
  public BulkheadRegistry bulkheadRegistry(
      final MeterRegistry meterRegistry,
      @Value("${resilience.bulkhead.max-concurrent-calls:128}") final int maxConcurrent,
      @Value("${resilience.bulkhead.max-wait-millis:100}") final long maxWaitMillis) {
    final var config = BulkheadConfig.custom()
      .maxConcurrentCalls(maxConcurrent)
      .maxWaitDuration(Duration.ofMillis(maxWaitMillis))
      .build();
    final var registry = BulkheadRegistry.of(config);
    TaggedBulkheadMetrics.ofBulkheadRegistry(registry).bindTo(meterRegistry);
    return registry;
  }

  @Bean
  @Primary
  public ProductRepository resilientProductRepository(
      final MongoProductRepositoryAdapter delegate,
      final CircuitBreakerRegistry circuitBreakerRegistry,
      final RetryRegistry retryRegistry,
      final BulkheadRegistry bulkheadRegistry) {
    final CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(RESILIENCE_LABEL);
    final Retry retry = retryRegistry.retry(RESILIENCE_LABEL);
    final Bulkhead bulkhead = bulkheadRegistry.bulkhead(RESILIENCE_LABEL);
    return new ResilientProductRepository(delegate, circuitBreaker, retry, bulkhead);
  }
}
