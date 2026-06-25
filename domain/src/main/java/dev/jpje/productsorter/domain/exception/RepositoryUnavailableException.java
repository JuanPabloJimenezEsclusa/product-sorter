package dev.jpje.productsorter.domain.exception;

public class RepositoryUnavailableException extends RuntimeException {

  public RepositoryUnavailableException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
