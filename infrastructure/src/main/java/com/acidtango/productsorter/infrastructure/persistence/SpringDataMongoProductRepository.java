package com.acidtango.productsorter.infrastructure.persistence;

import com.acidtango.productsorter.infrastructure.persistence.entity.ProductDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataMongoProductRepository extends MongoRepository<ProductDocument, String> {
}
