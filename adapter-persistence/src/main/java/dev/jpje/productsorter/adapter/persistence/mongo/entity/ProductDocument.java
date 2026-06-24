package dev.jpje.productsorter.adapter.persistence.mongo.entity;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("products")
public record ProductDocument(
    @Id String id,
    String name,
    int salesUnits,
    List<StockEntry> stock
) {
}
