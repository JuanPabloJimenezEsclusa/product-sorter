package dev.jpje.productsorter.domain.vo;

public record Size(String name) {
  public Size {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Size must not be blank");
    }
  }

  public static Size of(final String name) {
    return new Size(name);
  }
}
