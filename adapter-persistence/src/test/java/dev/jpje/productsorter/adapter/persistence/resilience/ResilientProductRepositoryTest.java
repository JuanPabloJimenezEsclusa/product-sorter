package dev.jpje.productsorter.adapter.persistence.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import dev.jpje.productsorter.domain.exception.RepositoryUnavailableException;
import dev.jpje.productsorter.domain.port.ProductRepository;
import dev.jpje.productsorter.domain.port.ProductRepository.PagedResult;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResilientProductRepositoryTest {

  private static final int LIMIT = 10;
  private static final int MAX_ATTEMPTS = 3;
  private static final PagedResult PAGE = new PagedResult(List.of(), null);

  @Mock
  private ProductRepository delegate;

  private CircuitBreaker circuitBreaker;
  private ResilientProductRepository repository;

  @BeforeEach
  void setUp() {
    circuitBreaker = CircuitBreaker.ofDefaults("mongo");
    final var retry = Retry.of("mongo", RetryConfig.custom()
      .maxAttempts(MAX_ATTEMPTS)
      .retryExceptions(RuntimeException.class)
      .build());
    final var bulkhead = Bulkhead.ofDefaults("mongo");
    repository = new ResilientProductRepository(delegate, circuitBreaker, retry, bulkhead);
  }

  @Test
  void shouldReturnResultWhenDelegateSucceeds() {
    when(delegate.findPage(null, LIMIT)).thenReturn(PAGE);

    assertThat(repository.findPage(null, LIMIT)).as("delegate result passes through").isSameAs(PAGE);
    verify(delegate, times(1)).findPage(null, LIMIT);
  }

  @Test
  void shouldRetryTransientFailureThenSucceed() {
    when(delegate.findPage(null, LIMIT))
      .thenThrow(new RuntimeException("blip"))
      .thenThrow(new RuntimeException("blip"))
      .thenReturn(PAGE);

    assertThat(repository.findPage(null, LIMIT)).as("succeeds after transient retries").isSameAs(PAGE);
    verify(delegate, times(MAX_ATTEMPTS)).findPage(null, LIMIT);
  }

  @Test
  void shouldFailFastWhenCircuitOpen() {
    circuitBreaker.transitionToOpenState();

    assertThatThrownBy(() -> repository.findPage(null, LIMIT))
      .as("open circuit fails fast as unavailable")
      .isInstanceOf(RepositoryUnavailableException.class);
    verify(delegate, times(0)).findPage(null, LIMIT);
  }
}
