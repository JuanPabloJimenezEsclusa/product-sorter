package dev.jpje.productsorter.domain.vo;

import java.io.Serializable;

public record Size(String name) implements Serializable {
  public Size {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Size must not be blank");
    }
  }

  public static Size of(final String name) {
    return new Size(name);
  }
}
