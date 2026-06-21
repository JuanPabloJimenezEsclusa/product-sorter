package com.acidtango.productsorter.config;

import com.acidtango.productsorter.application.service.ListProductsUseCaseImpl;
import com.acidtango.productsorter.application.service.SortProductsUseCaseImpl;
import com.acidtango.productsorter.domain.port.ListProductsUseCase;
import com.acidtango.productsorter.domain.port.ProductRepository;
import com.acidtango.productsorter.domain.port.SortProductsUseCase;
import com.acidtango.productsorter.domain.service.SortingEngine;
import com.acidtango.productsorter.infrastructure.observability.MetricsSortProductsUseCase;
import com.acidtango.productsorter.infrastructure.observability.SortingMetrics;
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
