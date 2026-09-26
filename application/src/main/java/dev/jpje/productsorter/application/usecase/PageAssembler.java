package dev.jpje.productsorter.application.usecase;

import java.util.List;
import java.util.function.ToDoubleFunction;

import dev.jpje.productsorter.application.port.ProductPageResult;
import dev.jpje.productsorter.domain.model.Product;
import dev.jpje.productsorter.domain.vo.CursorCodec;

/**
 * Single over-fetch trim and continuation-token producer shared by both catalog read paths. The
 * caller over-fetches one row past the requested size; this trims back to the visible page and encodes
 * the token from the last visible product, so the token has exactly one producer.
 */
final class PageAssembler {

  private PageAssembler() {
  }

  static ProductPageResult assemble(final List<Product> fetched, final int size,
                                    final ToDoubleFunction<Product> cursorScore) {
    final var hasMore = fetched.size() > size;
    final var trimmed = hasMore ? List.copyOf(fetched.subList(0, size)) : List.copyOf(fetched);
    final var nextCursor = hasMore && !trimmed.isEmpty()
      ? CursorCodec.encode(cursorScore.applyAsDouble(trimmed.getLast()),
          trimmed.getLast().productId().value())
      : null;
    return new ProductPageResult(trimmed, nextCursor, size);
  }
}
