package dev.jpje.productsorter.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CursorCodecTest {

  @ParameterizedTest(name = "{0}")
  @MethodSource("encodeDecodeScenarios")
  void shouldEncodeAndDecode(final double score, final String productId) {
    final var encoded = CursorCodec.encode(score, productId);
    assertThat(encoded).isNotEmpty();

    final var decoded = CursorCodec.decode(encoded);
    assertThat(decoded.score()).isEqualTo(score);
    assertThat(decoded.productId()).isEqualTo(productId);
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

  private static Stream<Arguments> invalidDecodeScenarios() {
    return Stream.of(
      arguments(named("null", (Object) null)),
      arguments(named("blank", "")),
      arguments(named("not base64", "!!invalid!!")));
  }
}
