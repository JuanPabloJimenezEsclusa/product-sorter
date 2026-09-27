package dev.jpje.productsorter.application.port;

/**
 * Single owner of the page-size rule: the default, the contract bounds, and the decision to reject an
 * out-of-range value instead of clamping it. Both catalog read paths resolve their size here so the
 * list and sort operations cannot diverge.
 */
public final class PageSize {

  public static final int DEFAULT = 20;
  public static final int MIN = 1;
  public static final int MAX = 100;

  private PageSize() {
  }

  public static int resolve(final Integer size) {
    if (size == null) {
      return DEFAULT;
    }
    if (size < MIN || size > MAX) {
      throw new IllegalArgumentException("Invalid page size");
    }
    return size;
  }
}
