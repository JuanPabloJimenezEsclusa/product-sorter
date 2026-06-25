package dev.jpje.productsorter.adapter.persistence.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.IOException;
import java.util.stream.Stream;

import com.mongodb.MongoExecutionTimeoutException;
import com.mongodb.MongoNodeIsRecoveringException;
import com.mongodb.MongoNotPrimaryException;
import com.mongodb.MongoOperationTimeoutException;
import com.mongodb.MongoSocketOpenException;
import com.mongodb.MongoSocketReadTimeoutException;
import com.mongodb.ServerAddress;
import org.bson.BsonDocument;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;

class MongoFaultClassifierTest {

  private static final ServerAddress ADDRESS = new ServerAddress();

  @ParameterizedTest(name = "{0}")
  @MethodSource("timeoutFaults")
  void shouldNotRetryTimeouts(final Throwable fault) {
    assertThat(MongoFaultClassifier.isRetryable(fault))
      .as("timeouts consumed their full budget and must not be retried").isFalse();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("connectivityFaults")
  void shouldRetryConnectivityAndFailoverFaults(final Throwable fault) {
    assertThat(MongoFaultClassifier.isRetryable(fault))
      .as("transient connectivity/failover faults are retryable").isTrue();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("nonTransientFaults")
  void shouldNotRetryNonTransientFaults(final Throwable fault) {
    assertThat(MongoFaultClassifier.isRetryable(fault))
      .as("non-transient faults must not be retried").isFalse();
  }

  private static Stream<Arguments> timeoutFaults() {
    return Stream.of(
      arguments(named("operation timeout (CSOT)", new MongoOperationTimeoutException("timeout"))),
      arguments(named("execution timeout (maxTimeMS)", new MongoExecutionTimeoutException(50, "exceeded"))),
      arguments(named("socket read timeout", new MongoSocketReadTimeoutException("read", ADDRESS, new IOException()))),
      arguments(named("spring query timeout", new QueryTimeoutException("slow query"))),
      arguments(named("spring-wrapped operation timeout",
        new DataAccessResourceFailureException("wrapped", new MongoOperationTimeoutException("timeout")))));
  }

  private static Stream<Arguments> connectivityFaults() {
    return Stream.of(
      arguments(named("not primary (failover)", new MongoNotPrimaryException(new BsonDocument(), ADDRESS))),
      arguments(named("node recovering", new MongoNodeIsRecoveringException(new BsonDocument(), ADDRESS))),
      arguments(named("socket open failure", new MongoSocketOpenException("connect", ADDRESS, new IOException()))),
      arguments(named("spring-wrapped not primary",
        new DataAccessResourceFailureException("wrapped", new MongoNotPrimaryException(new BsonDocument(), ADDRESS)))),
      arguments(named("resource failure", new DataAccessResourceFailureException("network down"))));
  }

  private static Stream<Arguments> nonTransientFaults() {
    return Stream.of(
      arguments(named("illegal argument", new IllegalArgumentException("bad weights"))),
      arguments(named("runtime", new RuntimeException("boom"))));
  }
}
