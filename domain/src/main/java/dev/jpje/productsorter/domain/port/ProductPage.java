package dev.jpje.productsorter.domain.port;

import java.util.List;

import dev.jpje.productsorter.domain.model.Product;

/**
 * Raw adapter-facing page: the rows returned by a catalog read, with no client-facing continuation
 * token. Ownership of the token belongs to the application-facing result, so this type deliberately
 * carries no cursor component and cannot become a second definition of the token.
 */
public record ProductPage(List<Product> products) {
  public ProductPage {
    products = List.copyOf(products);
  }
}
