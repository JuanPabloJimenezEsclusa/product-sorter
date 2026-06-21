package com.acidtango.productsorter.infrastructure.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document("products")
public class ProductDocument {

  @Id
  private String id;
  private String name;
  private int salesUnits;
  private List<StockEntry> stock;

  public ProductDocument() {
  }

  public ProductDocument(final String id, final String name, final int salesUnits, final List<StockEntry> stock) {
    this.id = id;
    this.name = name;
    this.salesUnits = salesUnits;
    this.stock = stock;
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public int getSalesUnits() {
    return salesUnits;
  }

  public void setSalesUnits(final int salesUnits) {
    this.salesUnits = salesUnits;
  }

  public List<StockEntry> getStock() {
    return stock;
  }

  public void setStock(final List<StockEntry> stock) {
    this.stock = stock;
  }

  public static class StockEntry {
    private String size;
    private int quantity;

    public StockEntry() {
    }

    public StockEntry(final String size, final int quantity) {
      this.size = size;
      this.quantity = quantity;
    }

    public String getSize() {
      return size;
    }

    public void setSize(final String size) {
      this.size = size;
    }

    public int getQuantity() {
      return quantity;
    }

    public void setQuantity(final int quantity) {
      this.quantity = quantity;
    }
  }
}
