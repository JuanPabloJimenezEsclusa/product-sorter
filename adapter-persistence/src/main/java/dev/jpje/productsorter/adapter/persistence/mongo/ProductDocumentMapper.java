package dev.jpje.productsorter.adapter.persistence.mongo;

import dev.jpje.productsorter.adapter.persistence.mongo.entity.ProductDocument;
import dev.jpje.productsorter.domain.model.Product;
import dev.jpje.productsorter.domain.vo.ProductId;
import dev.jpje.productsorter.domain.vo.ProductName;
import dev.jpje.productsorter.domain.vo.SalesUnits;
import dev.jpje.productsorter.domain.vo.Size;
import dev.jpje.productsorter.domain.vo.Stock;
import dev.jpje.productsorter.domain.vo.StockBySize;
import org.springframework.stereotype.Component;

@Component
public class ProductDocumentMapper {

  public Product toDomain(final ProductDocument doc) {
    return new Product(
      ProductId.of(doc.id()),
      ProductName.of(doc.name()),
      SalesUnits.of(doc.salesUnits()),
      Stock.of(doc.stock().stream()
        .map(e -> StockBySize.of(Size.of(e.size()), e.quantity()))
        .toList()),
      doc.weightedScore()
    );
  }
}
