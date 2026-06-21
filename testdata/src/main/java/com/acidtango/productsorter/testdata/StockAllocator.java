package com.acidtango.productsorter.testdata;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.random.RandomGenerator;

public class StockAllocator {

  private final RandomGenerator rng;

  public StockAllocator(final RandomGenerator rng) {
    this.rng = rng;
  }

  public Map<String, Integer> allocate() {
    final var stock = new LinkedHashMap<String, Integer>();
    final var outOfStock = rng.nextDouble() < 0.15;
    stock.put("S", outOfStock ? 0 : Math.max(0, (int) (rng.nextGaussian() * 10 + 20)));
    stock.put("M", Math.max(0, (int) (rng.nextGaussian() * 10 + 30)));
    stock.put("L", Math.max(0, (int) (rng.nextGaussian() * 5 + 10)));
    return stock;
  }
}
