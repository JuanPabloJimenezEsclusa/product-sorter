package dev.jpje.productsorter.adapter.persistence.mongo.datagen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

class StockAllocator {

  private static final String[] CLOTHING_SIZES = {"XS", "S", "M", "L", "XL", "XXL"};

  private final RandomGenerator rng;
  private final List<String> sizes;

  StockAllocator(final RandomGenerator rng) {
    this(rng, List.of(CLOTHING_SIZES));
  }

  StockAllocator(final RandomGenerator rng, final List<String> sizes) {
    this.rng = rng;
    this.sizes = List.copyOf(sizes);
  }

  Map<String, Integer> allocate() {
    final var stock = new LinkedHashMap<String, Integer>();
    final var outOfStockChance = rng.nextDouble();
    final var missingSizes = pickMissingSizes();

    for (final var size : sizes) {
      if (missingSizes.contains(size) || outOfStockChance < 0.05) {
        stock.put(size, 0);
      } else {
        stock.put(size, randomQuantity());
      }
    }
    return stock;
  }

  private List<String> pickMissingSizes() {
    final var missing = new ArrayList<String>();
    for (final var size : sizes) {
      if (rng.nextDouble() < 0.10) {
        missing.add(size);
      }
    }
    return missing;
  }

  private int randomQuantity() {
    final var base = rng.nextDouble() < 0.5 ? rng.nextGaussian() * 15 + 30 : rng.nextGaussian() * 5 + 5;
    return Math.max(0, (int) base);
  }
}
