package dev.jpje.productsorter.adapter.persistence.mongo.datagen;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.random.RandomGenerator;

class StockAllocator {

  private final RandomGenerator rng;

  StockAllocator(final RandomGenerator rng) {
    this.rng = rng;
  }

  Map<String, Integer> allocate() {
    final var stock = new LinkedHashMap<String, Integer>();
    final var outOfStock = rng.nextDouble() < 0.15;
    stock.put("S", outOfStock ? 0 : Math.max(0, (int) (rng.nextGaussian() * 10 + 20)));
    stock.put("M", Math.max(0, (int) (rng.nextGaussian() * 10 + 30)));
    stock.put("L", Math.max(0, (int) (rng.nextGaussian() * 5 + 10)));
    return stock;
  }
}
