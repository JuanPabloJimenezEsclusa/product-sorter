package dev.jpje.productsorter.adapter.rest.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.test.util.ReflectionTestUtils;

class SecurityConfigTest {

  @Test
  void shouldReturnStableUnauthorizedMessageWithoutEchoingExceptionText() throws Exception {
    final var entryPoint = (AuthenticationEntryPoint) ReflectionTestUtils.invokeMethod(
      SecurityConfig.class, "jwtErrorEntryPoint");
    final var request = new MockHttpServletRequest();
    final var response = new MockHttpServletResponse();

    entryPoint.commence(request, response, new BadCredentialsException("JWT signature: secret-detail-42"));

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentAsString())
      .as("a 401 body must carry a stable message and must not echo the authentication exception text")
      .contains("\"message\":\"Unauthorized\"")
      .doesNotContain("secret-detail-42");
  }
}
