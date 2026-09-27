package dev.jpje.productsorter.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CursorCodecTest {

  @ParameterizedTest(name = "{0}")
  @MethodSource("encodeDecodeScenarios")
  void shouldEncodeAndDecode(final double score, final String productId) {
    final var encoded = CursorCodec.encode(score, productId);
    assertThat(encoded).as("encoded cursor not empty").isNotEmpty();

    final var decoded = CursorCodec.decode(encoded);
    assertThat(decoded.score()).as("round-trip score").isEqualTo(score);
    assertThat(decoded.productId()).as("round-trip product id").isEqualTo(productId);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("nonAsciiScenarios")
  void shouldRoundTripNonAsciiProductIds(final String productId) {
    final var encoded = CursorCodec.encode(0.5, productId);
    assertThat(CursorCodec.decode(encoded).productId())
      .as("non-ASCII product id round-trips byte-for-byte")
      .isEqualTo(productId);
  }

  @Test
  void shouldEncodeWithUtf8RegardlessOfThePlatformDefaultCharset() {
    final var productId = "café-产品-🎯";
    final var expected = Base64.getEncoder()
      .encodeToString(("0.5:" + productId).getBytes(StandardCharsets.UTF_8));

    assertThat(CursorCodec.encode(0.5, productId))
      .as("cursor must be UTF-8 encoded")
      .isEqualTo(expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidDecodeScenarios")
  void shouldRejectInvalidCursor(final String encoded) {
    assertThatThrownBy(() -> CursorCodec.decode(encoded))
      .isInstanceOf(IllegalArgumentException.class);
  }

  private static Stream<Arguments> encodeDecodeScenarios() {
    return Stream.of(
      arguments(named("positive score", 455.1), "id5"),
      arguments(named("zero score", 0.0), "id1"),
      arguments(named("negative score", -1.0), "id99"));
  }

  private static Stream<Arguments> nonAsciiScenarios() {
    return Stream.of(
      arguments(named("latin accent", "café-1")),
      arguments(named("cjk", "产品-42")),
      arguments(named("emoji", "prod-🎯")));
  }

  private static Stream<Arguments> invalidDecodeScenarios() {
    return Stream.of(
      arguments(named("null", (Object) null)),
      arguments(named("blank", "")),
      arguments(named("not base64", "!!invalid!!")));
  }
}
