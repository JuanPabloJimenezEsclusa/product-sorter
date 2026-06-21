package com.acidtango.productsorter.testdata;

import java.util.List;
import java.util.random.RandomGenerator;

public class RealisticProductNames {

  private static final List<String> STYLES = List.of(
    "CLASSIC", "MODERN", "VINTAGE", "PREMIUM", "ESSENTIAL",
    "URBAN", "RUSTIC", "ATHLETIC", "CASUAL", "FORMAL",
    "LUXURY", "LIGHT-WEIGHT", "HEAVY-DUTY", "SLIM FIT", "RELAXED",
    "OVERSIZED", "TAILORED", "ACTIVE", "HERITAGE", "CONTEMPORARY"
  );

  private static final List<String> FABRICS = List.of(
    "COTTON", "LINEN", "DENIM", "SILK", "VELVET",
    "CORDUROY", "FLANNEL", "SEERSUCKER", "JERSEY", "PIQUE",
    "TWILL", "CHAMBRAY", "SATIN", "SLUB", "ORGANIC COTTON",
    "RECYCLED POLY", "BRODERIE", "MELANGE", "OXFORD", "POPLIN"
  );

  private static final List<String> PATTERNS = List.of(
    "STRIPED", "PLAID", "GRAPHIC", "EMBROIDERED",
    "CAMOUFLAGE", "BATIK", "ABSTRACT", "MINIMALIST", "RETRO",
    "PATCHWORK", "DISTRESSED", "PREPPY", "BOHEMIAN", "TRIBAL",
    "GEOMETRIC", "FLORAL", "ANIMAL", "TIE-DYE", "ARGYLE", "IKAT"
  );

  private static final List<String> NECKLINES = List.of(
    "V-NECK", "CREW NECK", "MOCK NECK", "BAND COLLAR",
    "CAMP COLLAR", "SPREAD COLLAR", "BUTTON DOWN", "HENLEY NECK",
    "SCOOP NECK", "BOAT NECK", "COWL NECK", "MANDARIN COLLAR",
    "TURTLENECK", "RINGER NECK", "SCOOP BACK", "KEYHOLE NECK"
  );

  private static final List<String> GARMENTS = List.of(
    "T-SHIRT", "SHIRT", "POLO", "HENLEY", "SWEATER",
    "CARDIGAN", "TANK TOP", "HOODIE", "SWEATSHIRT", "BLOUSE",
    "VEST", "PULLOVER", "JACKET", "TEE", "TOP",
    "TUNIC", "BODICE", "RAGLAN", "JERSEY", "SHELL"
  );

  private final RandomGenerator rng;

  public RealisticProductNames(final RandomGenerator rng) {
    this.rng = rng;
  }

  public String sample() {
    final var parts = new java.util.ArrayList<String>();
    if (rng.nextBoolean()) {
      parts.add(STYLES.get(rng.nextInt(STYLES.size())));
    }
    if (rng.nextDouble() < 0.65) {
      parts.add(FABRICS.get(rng.nextInt(FABRICS.size())));
    }
    if (rng.nextDouble() < 0.35) {
      parts.add(PATTERNS.get(rng.nextInt(PATTERNS.size())));
    }
    if (rng.nextDouble() < 0.5) {
      parts.add(NECKLINES.get(rng.nextInt(NECKLINES.size())));
    }
    parts.add(GARMENTS.get(rng.nextInt(GARMENTS.size())));
    return String.join(" ", parts);
  }
}
