package com.acidtango.productsorter.infrastructure.persistence;

import java.util.List;

import com.acidtango.productsorter.domain.model.Product;
import com.acidtango.productsorter.domain.port.ProductRepository;
import org.springframework.stereotype.Repository;

@Repository
public class MongoProductRepositoryAdapter implements ProductRepository {

  private final SpringDataMongoProductRepository springRepo;
  private final ProductDocumentMapper mapper;

  public MongoProductRepositoryAdapter(final SpringDataMongoProductRepository springRepo,
                                       final ProductDocumentMapper mapper) {
    this.springRepo = springRepo;
    this.mapper = mapper;
  }

  @Override
  public List<Product> findAll() {
    return springRepo.findAll().stream()
      .map(mapper::toDomain)
      .toList();
  }
}
