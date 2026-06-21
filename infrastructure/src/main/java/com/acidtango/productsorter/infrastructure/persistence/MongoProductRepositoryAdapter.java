package com.acidtango.productsorter.infrastructure.persistence;

import com.acidtango.productsorter.domain.model.*;
import com.acidtango.productsorter.domain.port.ProductRepository;
import com.acidtango.productsorter.infrastructure.persistence.entity.ProductDocument;
import com.acidtango.productsorter.infrastructure.persistence.entity.ProductDocument.StockEntry;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MongoProductRepositoryAdapter implements ProductRepository {

  private final SpringDataMongoProductRepository springRepo;

  public MongoProductRepositoryAdapter(final SpringDataMongoProductRepository springRepo) {
    this.springRepo = springRepo;
  }

  @Override
  public List<Product> findAll() {
    return springRepo.findAll().stream()
      .map(this::toDomain)
      .toList();
  }

  public void saveAll(final List<Product> products) {
    final var docs = products.stream()
      .map(this::toDocument)
      .toList();
    springRepo.saveAll(docs);
  }

  private Product toDomain(final ProductDocument doc) {
    return new Product(
      ProductId.of(Long.parseLong(doc.getId())),
      ProductName.of(doc.getName()),
      SalesUnits.of(doc.getSalesUnits()),
      Stock.of(doc.getStock().stream()
        .map(e -> StockBySize.of(Size.valueOf(e.getSize()), e.getQuantity()))
        .toList())
    );
  }

  private ProductDocument toDocument(final Product product) {
    return new ProductDocument(
      String.valueOf(product.productId().value()),
      product.productName().value(),
      product.salesUnits().value(),
      product.stock().entries().stream()
        .map(s -> new StockEntry(s.size().name(), s.quantity()))
        .toList()
    );
  }
}
