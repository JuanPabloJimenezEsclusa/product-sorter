package dev.jpje.productsorter.adapter.observability;

import io.micrometer.java21.instrument.binder.jdk.VirtualThreadMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VirtualThreadMetricsConfig {

  @Bean
  public VirtualThreadMetrics virtualThreadMetrics() {
    return new VirtualThreadMetrics();
  }

  @Bean
  public VirtualThreadCountMetrics virtualThreadCountMetrics() {
    return new VirtualThreadCountMetrics();
  }
}
