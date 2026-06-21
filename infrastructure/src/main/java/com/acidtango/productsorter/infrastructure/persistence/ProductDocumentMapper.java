package com.acidtango.productsorter.infrastructure.persistence;

import com.acidtango.productsorter.domain.model.Product;
import com.acidtango.productsorter.domain.model.ScoreableProduct;
import com.acidtango.productsorter.domain.vo.ProductId;
import com.acidtango.productsorter.domain.vo.ProductName;
import com.acidtango.productsorter.domain.vo.SalesUnits;
import com.acidtango.productsorter.domain.vo.Size;
import com.acidtango.productsorter.domain.vo.Stock;
import com.acidtango.productsorter.domain.vo.StockBySize;
import com.acidtango.productsorter.infrastructure.persistence.entity.ProductDocument;
import org.bson.Document;
import org.springframework.stereotype.Component;

@Component
public class ProductDocumentMapper {

  public Product toDomain(final ProductDocument doc) {
    return new Product(
      ProductId.of(doc.id()),
      ProductName.of(doc.name()),
      SalesUnits.of(doc.salesUnits()),
      Stock.of(doc.stock().stream()
        .map(e -> StockBySize.of(Size.valueOf(e.size()), e.quantity()))
        .toList())
    );
  }

  public ScoreableProduct toScoreable(final Document doc) {
    final var stockEntries = doc.getList("stock", Document.class);
    final var sizesWithStock = stockEntries != null
      ? stockEntries.stream().filter(e -> e.get("quantity", Number.class).intValue() > 0).count()
      : 0L;
    final var totalSizes = stockEntries != null ? stockEntries.size() : 1;
    final var ratio = (double) sizesWithStock / totalSizes;

    return new ScoreableProduct(
      ProductId.of(doc.get("_id").toString()),
      doc.getInteger("salesUnits", 0),
      ratio
    );
  }
}
