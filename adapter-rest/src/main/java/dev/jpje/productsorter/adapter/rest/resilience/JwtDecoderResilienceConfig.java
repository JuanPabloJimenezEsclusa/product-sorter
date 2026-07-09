package dev.jpje.productsorter.adapter.rest.resilience;

import java.time.Duration;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

@Configuration
public class JwtDecoderResilienceConfig {

  @Bean
  public JwtDecoder resilientJwtDecoder(
      final MeterRegistry meterRegistry,
      @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:http://localhost:8081/realms/product-sorter/protocol/openid-connect/certs}") final String jwkSetUri,
      @Value("${resilience.jwks.max-attempts:2}") final int maxAttempts,
      @Value("${resilience.jwks.connect-timeout-seconds:2}") final long connectTimeoutSeconds,
      @Value("${resilience.jwks.read-timeout-seconds:3}") final long readTimeoutSeconds) {
    final var retryConfig = RetryConfig.custom()
      .maxAttempts(maxAttempts)
      .intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.ofMillis(200), 2.0))
      .retryExceptions(ResourceAccessException.class, HttpServerErrorException.class)
      .build();
    final var registry = RetryRegistry.of(retryConfig);
    TaggedRetryMetrics.ofRetryRegistry(registry).bindTo(meterRegistry);
    final Retry jwksRetry = registry.retry("jwks");

    final var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds));
    factory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));

    final RestOperations restOperations = new RestTemplate(factory) {
      @Override
      @NonNull
      public <T> ResponseEntity<T> exchange(final @NonNull RequestEntity<?> requestEntity,
                                            final @NonNull Class<T> responseType) {
        return jwksRetry.executeSupplier(() -> super.exchange(requestEntity, responseType));
      }
    };

    return NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
      .restOperations(restOperations)
      .build();
  }
}
