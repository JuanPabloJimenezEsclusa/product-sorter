package dev.jpje.productsorter.adapter.rest.config;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.jpje.productsorter.api.v1.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
  private static final ObjectMapper MAPPER = new ObjectMapper()
    .registerModule(new JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  @Bean
  @SuppressWarnings("java:S4502")
  public SecurityFilterChain filterChain(final HttpSecurity http) {
    http.csrf(AbstractHttpConfigurer::disable)
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
        .requestMatchers(
          "/swagger-ui/**",
          "/swagger-ui.html",
          "/v3/api-docs/**",
          "/v3/api-docs",
          "/actuator/health",
          "/actuator/info",
          "/actuator/prometheus"
        ).permitAll()
        .requestMatchers("/api/**").authenticated()
        .anyRequest().authenticated()
      )
      .oauth2ResourceServer(oauth2 -> oauth2
        .jwt(_ -> {})
        .authenticationEntryPoint(jwtErrorEntryPoint()));
    return http.build();
  }

  private static AuthenticationEntryPoint jwtErrorEntryPoint() {
    return (_, response, authException) -> {
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setStatus(HttpStatus.UNAUTHORIZED.value());
      try (final var writer = response.getWriter()) {
        MAPPER.writeValue(writer, new ErrorResponse()
          .status(HttpStatus.UNAUTHORIZED.value())
          .code(HttpStatus.UNAUTHORIZED.name())
          .message(authException.getMessage())
          .timestamp(OffsetDateTime.now(ZoneId.systemDefault())));
      } catch (final IOException e) {
        log.error(e.getMessage(), e);
        response.sendError(HttpStatus.UNAUTHORIZED.value(), authException.getMessage());
      }
    };
  }
}
