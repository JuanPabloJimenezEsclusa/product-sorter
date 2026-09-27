package dev.jpje.productsorter.adapter.persistence.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import dev.jpje.productsorter.domain.exception.RepositoryUnavailableException;
import dev.jpje.productsorter.domain.port.ProductPage;
import dev.jpje.productsorter.domain.port.ProductRepository;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;

@ExtendWith(MockitoExtension.class)
class ResilientProductRepositoryTest {

  private static final int LIMIT = 10;
  private static final int MAX_ATTEMPTS = 3;
  private static final ProductPage PAGE = new ProductPage(List.of());

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

  @Test
  void shouldFailFastWhenBulkheadIsFull() {
    final var bulkhead = Bulkhead.of("mongo", BulkheadConfig.custom()
      .maxConcurrentCalls(1)
      .maxWaitDuration(Duration.ZERO)
      .build());
    final var saturated = new ResilientProductRepository(delegate, circuitBreaker, retry(), bulkhead);
    assertThat(bulkhead.tryAcquirePermission()).as("the only concurrent slot is taken").isTrue();
    try {
      assertThatThrownBy(() -> saturated.findPage(null, LIMIT))
        .as("a full bulkhead fails fast as unavailable")
        .isInstanceOf(RepositoryUnavailableException.class);
    } finally {
      bulkhead.releasePermission();
    }
    verify(delegate, times(0)).findPage(null, LIMIT);
  }

  @Test
  void shouldTranslateExhaustedRetryDataAccessFailure() {
    final var fault = new DataAccessResourceFailureException("mongo down");
    when(delegate.findPage(null, LIMIT)).thenThrow(fault);

    assertThatThrownBy(() -> faultTypedRepository().findPage(null, LIMIT))
      .as("an exhausted retryable data-access fault is translated to unavailability")
      .isInstanceOf(RepositoryUnavailableException.class)
      .hasCause(fault);
    verify(delegate, times(MAX_ATTEMPTS)).findPage(null, LIMIT);
  }

  @Test
  void shouldTranslateNonRetriedQueryTimeout() {
    final var fault = new QueryTimeoutException("slow query");
    when(delegate.findPage(null, LIMIT)).thenThrow(fault);

    assertThatThrownBy(() -> faultTypedRepository().findPage(null, LIMIT))
      .as("a non-retried data-access fault is translated to unavailability")
      .isInstanceOf(RepositoryUnavailableException.class)
      .hasCause(fault);
    verify(delegate, times(1)).findPage(null, LIMIT);
  }

  @Test
  void shouldNotTranslateNonDataAccessFailure() {
    when(delegate.findPage(null, LIMIT)).thenThrow(new IllegalStateException("boom"));

    assertThatThrownBy(() -> repository.findPage(null, LIMIT))
      .as("a non-data-access failure propagates unchanged and stays a server error")
      .isInstanceOf(IllegalStateException.class)
      .isNotInstanceOf(RepositoryUnavailableException.class)
      .hasMessage("boom");
  }

  @Test
  void shouldComposeTheDecoratorChainOnce() {
    final var retry = Retry.ofDefaults("mongo");
    final var bulkhead = Bulkhead.ofDefaults("mongo");
    when(delegate.findPage(null, LIMIT)).thenReturn(PAGE);

    try (MockedStatic<Retry> retryStatic = mockStatic(Retry.class, CALLS_REAL_METHODS);
         MockedStatic<CircuitBreaker> breakerStatic = mockStatic(CircuitBreaker.class, CALLS_REAL_METHODS);
         MockedStatic<Bulkhead> bulkheadStatic = mockStatic(Bulkhead.class, CALLS_REAL_METHODS)) {
      final var composed = new ResilientProductRepository(delegate, circuitBreaker, retry, bulkhead);
      composed.findPage(null, LIMIT);
      composed.findPage(null, LIMIT);

      retryStatic.verify(() -> Retry.decorateSupplier(eq(retry), any()), times(1));
      breakerStatic.verify(() -> CircuitBreaker.decorateSupplier(eq(circuitBreaker), any()), times(1));
      bulkheadStatic.verify(() -> Bulkhead.decorateSupplier(eq(bulkhead), any()), times(1));
    }
  }

  private ResilientProductRepository faultTypedRepository() {
    final var retry = Retry.of("mongo", RetryConfig.custom()
      .maxAttempts(MAX_ATTEMPTS)
      .retryOnException(MongoFaultClassifier::isRetryable)
      .build());
    return new ResilientProductRepository(delegate, circuitBreaker, retry, Bulkhead.ofDefaults("mongo"));
  }

  private static Retry retry() {
    return Retry.ofDefaults("mongo");
  }
}
