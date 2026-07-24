package dev.jpje.productsorter.config;

import dev.jpje.productsorter.adapter.observability.SortProductsMetricsDecorator;
import dev.jpje.productsorter.adapter.observability.SortingMetrics;
import dev.jpje.productsorter.application.port.ListProducts;
import dev.jpje.productsorter.application.port.SortProducts;
import dev.jpje.productsorter.application.usecase.ListProductsUseCase;
import dev.jpje.productsorter.application.usecase.SortProductsUseCase;
import dev.jpje.productsorter.domain.port.ProductRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

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
}
