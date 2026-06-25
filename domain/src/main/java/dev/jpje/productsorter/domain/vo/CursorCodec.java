package dev.jpje.productsorter.domain.vo;

import java.util.Base64;

public final class CursorCodec {
  private static final Base64.Decoder DECODER = Base64.getDecoder();
  private static final Base64.Encoder ENCODER = Base64.getEncoder();

  private CursorCodec() {
  }

  public static String encode(final double score, final String productId) {
    return ENCODER.encodeToString((score + ":" + productId).getBytes());
  }

  public static DecodedCursor decode(final String encoded) {
    if (encoded == null || encoded.isBlank()) {
      throw new IllegalArgumentException("Cursor must not be blank");
    }
    final var parts = new String(DECODER.decode(encoded)).split(":", 2);
    if (parts.length != 2) {
      throw new IllegalArgumentException("Invalid cursor format");
    }
    return new DecodedCursor(Double.parseDouble(parts[0]), parts[1]);
  }

  public record DecodedCursor(double score, String productId) {
  }
}
