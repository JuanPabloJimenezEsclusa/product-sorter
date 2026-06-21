package com.acidtango.productsorter.testdata;

import java.util.random.RandomGenerator;

public class SalesDistribution {

  private final RandomGenerator rng;

  public SalesDistribution(final RandomGenerator rng) {
    this.rng = rng;
  }

  public int sample() {
    final var uniform = rng.nextDouble();
    final var raw = Math.pow(uniform, 2.5) * 1000;
    return Math.max(0, (int) Math.round(raw));
  }
}
