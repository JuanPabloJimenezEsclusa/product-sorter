package dev.jpje.productsorter.adapter.persistence.resilience;

import java.util.function.Supplier;

import dev.jpje.productsorter.domain.exception.RepositoryUnavailableException;
import dev.jpje.productsorter.domain.model.AppliedWeights;
import dev.jpje.productsorter.domain.port.ProductRepository;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;

public class ResilientProductRepository implements ProductRepository {

  private final ProductRepository delegate;
  private final CircuitBreaker circuitBreaker;
  private final Retry retry;
  private final Bulkhead bulkhead;

  public ResilientProductRepository(final ProductRepository delegate,
                                    final CircuitBreaker circuitBreaker,
                                    final Retry retry,
                                    final Bulkhead bulkhead) {
    this.delegate = delegate;
    this.circuitBreaker = circuitBreaker;
    this.retry = retry;
    this.bulkhead = bulkhead;
  }

  @Override
  public PagedResult findPage(final String cursor, final int limit) {
    return call(() -> delegate.findPage(cursor, limit));
  }

  @Override
  public PagedResult sortByWeights(final AppliedWeights weights, final String cursor, final int limit) {
    return call(() -> delegate.sortByWeights(weights, cursor, limit));
  }

  private <T> T call(final Supplier<T> action) {
    Supplier<T> decorated = Retry.decorateSupplier(retry, action);
    decorated = CircuitBreaker.decorateSupplier(circuitBreaker, decorated);
    decorated = Bulkhead.decorateSupplier(bulkhead, decorated);
    try {
      return decorated.get();
    } catch (final CallNotPermittedException | BulkheadFullException ex) {
      throw new RepositoryUnavailableException("Product repository temporarily unavailable", ex);
    }
  }
}
