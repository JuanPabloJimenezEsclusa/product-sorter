package dev.jpje.productsorter.config;

import dev.jpje.productsorter.adapter.observability.SortProductsMetricsDecorator;
import dev.jpje.productsorter.adapter.observability.SortingMetrics;
import dev.jpje.productsorter.adapter.persistence.mongo.MongoProductRepositoryAdapter;
import dev.jpje.productsorter.adapter.persistence.resilience.ResilientProductRepository;
import dev.jpje.productsorter.application.port.ListProducts;
import dev.jpje.productsorter.application.port.SortProducts;
import dev.jpje.productsorter.application.usecase.ListProductsUseCase;
import dev.jpje.productsorter.application.usecase.SortProductsUseCase;
import dev.jpje.productsorter.domain.port.ProductRepository;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ApplicationConfig {

  private static final String RESILIENT_KEY = "mongo";

  @Bean
  public SortProducts sortProductsUseCase(final ProductRepository repository,
                                          final SortingMetrics metrics) {
    final var usecase = new SortProductsUseCase(repository);
    return new SortProductsMetricsDecorator(usecase, metrics);
  }

  @Bean
  public ListProducts listProductsUseCase(final ProductRepository repository) {
    return new ListProductsUseCase(repository);
  }

  @Bean
  @Primary
  public ProductRepository resilientProductRepository(
      final MongoProductRepositoryAdapter delegate,
      final CircuitBreakerRegistry circuitBreakerRegistry,
      final RetryRegistry retryRegistry,
      final BulkheadRegistry bulkheadRegistry) {
    return new ResilientProductRepository(delegate,
      circuitBreakerRegistry.circuitBreaker(RESILIENT_KEY),
      retryRegistry.retry(RESILIENT_KEY),
      bulkheadRegistry.bulkhead(RESILIENT_KEY));
  }
}
