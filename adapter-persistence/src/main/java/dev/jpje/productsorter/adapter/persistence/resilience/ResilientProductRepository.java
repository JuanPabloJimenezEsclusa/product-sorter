package dev.jpje.productsorter.adapter.persistence.resilience;

import java.util.function.Supplier;

import dev.jpje.productsorter.domain.exception.RepositoryUnavailableException;
import dev.jpje.productsorter.domain.model.AppliedWeights;
import dev.jpje.productsorter.domain.port.ProductPage;
import dev.jpje.productsorter.domain.port.ProductRepository;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.springframework.dao.DataAccessException;

public class ResilientProductRepository implements ProductRepository {

  private final ProductRepository delegate;
  private final Supplier<Object> resilienceChain;
  private final ThreadLocal<Supplier<Object>> actionHolder = new ThreadLocal<>();

  public ResilientProductRepository(final ProductRepository delegate,
                                    final CircuitBreaker circuitBreaker,
                                    final Retry retry,
                                    final Bulkhead bulkhead) {
    this.delegate = delegate;
    this.resilienceChain = Bulkhead.decorateSupplier(bulkhead,
      CircuitBreaker.decorateSupplier(circuitBreaker,
        Retry.decorateSupplier(retry, () -> actionHolder.get().get())));
  }

  @Override
  public ProductPage findPage(final String cursor, final int limit) {
    return call(() -> delegate.findPage(cursor, limit));
  }

  @Override
  public ProductPage sortByWeights(final AppliedWeights weights, final String cursor, final int limit) {
    return call(() -> delegate.sortByWeights(weights, cursor, limit));
  }

  @SuppressWarnings("unchecked")
  private <T> T call(final Supplier<T> action) {
    actionHolder.set((Supplier<Object>) action);
    try {
      return (T) resilienceChain.get();
    } catch (final CallNotPermittedException | BulkheadFullException | DataAccessException ex) {
      throw new RepositoryUnavailableException("Product repository temporarily unavailable", ex);
    } finally {
      actionHolder.remove();
    }
  }
}
