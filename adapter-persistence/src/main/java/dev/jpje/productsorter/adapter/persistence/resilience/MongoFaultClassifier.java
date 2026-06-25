package dev.jpje.productsorter.adapter.persistence.resilience;

import com.mongodb.MongoExecutionTimeoutException;
import com.mongodb.MongoNodeIsRecoveringException;
import com.mongodb.MongoNotPrimaryException;
import com.mongodb.MongoSocketException;
import com.mongodb.MongoSocketReadTimeoutException;
import com.mongodb.MongoTimeoutException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;

final class MongoFaultClassifier {

  private MongoFaultClassifier() {
  }

  static boolean isRetryable(final Throwable throwable) {
    if (hasCause(throwable, MongoTimeoutException.class)
        || hasCause(throwable, MongoExecutionTimeoutException.class)
        || hasCause(throwable, MongoSocketReadTimeoutException.class)
        || throwable instanceof QueryTimeoutException) {
      return false;
    }
    return hasCause(throwable, MongoNotPrimaryException.class)
      || hasCause(throwable, MongoNodeIsRecoveringException.class)
      || hasCause(throwable, MongoSocketException.class)
      || throwable instanceof TransientDataAccessException
      || throwable instanceof DataAccessResourceFailureException;
  }

  private static boolean hasCause(final Throwable throwable, final Class<? extends Throwable> type) {
    for (Throwable current = throwable; current != null; current = current.getCause()) {
      if (type.isInstance(current)) {
        return true;
      }
    }
    return false;
  }
}
