package dev.jpje.productsorter.adapter.persistence.mongo.datagen;

import java.util.random.RandomGenerator;

class SalesDistribution {

  private final RandomGenerator rng;

  SalesDistribution(final RandomGenerator rng) {
    this.rng = rng;
  }

  int sample() {
    final var uniform = rng.nextDouble();
    final var raw = Math.pow(uniform, 2.5) * 1000;
    return Math.max(0, (int) Math.round(raw));
  }
}
