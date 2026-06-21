package com.acidtango.productsorter.infrastructure.persistence;

import com.acidtango.productsorter.domain.model.Product;
import com.acidtango.productsorter.domain.model.ScoreableProduct;
import com.acidtango.productsorter.domain.vo.*;
import com.acidtango.productsorter.infrastructure.persistence.entity.ProductDocument;
import org.springframework.stereotype.Component;

@Component
public class ProductDocumentMapper {

  public Product toDomain(final ProductDocument doc) {
    return new Product(
      ProductId.of(Long.parseLong(doc.id())),
      ProductName.of(doc.name()),
      SalesUnits.of(doc.salesUnits()),
      Stock.of(doc.stock().stream()
        .map(e -> StockBySize.of(Size.valueOf(e.size()), e.quantity()))
        .toList())
    );
  }

  public ScoreableProduct toScoreable(final ProductDocument doc) {
    final var stock = Stock.of(doc.stock().stream()
      .map(e -> StockBySize.of(Size.valueOf(e.size()), e.quantity()))
      .toList());
    return new ScoreableProduct(
      ProductId.of(Long.parseLong(doc.id())),
      doc.salesUnits(),
      stock.ratio()
    );
  }
}
