package dev.jpje.productsorter.config;

import dev.jpje.productsorter.adapter.observability.MetricsSortProductsUseCase;
import dev.jpje.productsorter.adapter.observability.SortingMetrics;
import dev.jpje.productsorter.application.port.ListProductsUseCase;
import dev.jpje.productsorter.application.port.SortProductsUseCase;
import dev.jpje.productsorter.application.service.ListProductsUseCaseImpl;
import dev.jpje.productsorter.application.service.SortProductsUseCaseImpl;
import dev.jpje.productsorter.domain.port.ProductRepository;
import dev.jpje.productsorter.domain.service.SortingEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

  @Bean
  public SortingEngine sortingEngine() {
    return new SortingEngine();
  }

  @Bean
  public SortProductsUseCase sortProductsUseCase(final ProductRepository repository,
                                                 final SortingEngine engine,
                                                 final SortingMetrics metrics) {
    final var impl = new SortProductsUseCaseImpl(repository, engine);
    return new MetricsSortProductsUseCase(impl, metrics);
  }

  @Bean
  public ListProductsUseCase listProductsUseCase(final ProductRepository repository) {
    return new ListProductsUseCaseImpl(repository);
  }
}
