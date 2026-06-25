package dev.jpje.productsorter.adapter.rest.config;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.jpje.productsorter.api.v1.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
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
          "/api/v1/swagger-ui/**",
          "/api/v1/v3/api-docs/**",
          "/favicon.ico",
          "/actuator/health",
          "/actuator/health/**",
          "/actuator/prometheus",
          "/actuator/metrics",
          "/actuator/metrics/**"
        ).permitAll()
        .requestMatchers("/api/**").authenticated()
        .anyRequest().authenticated()
      )
      .oauth2ResourceServer(oauth2 -> oauth2
        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter()))
        .authenticationEntryPoint(jwtErrorEntryPoint()));
    return http.build();
  }

  private Converter<Jwt, AbstractAuthenticationToken> keycloakJwtAuthenticationConverter() {
    return jwt -> {

      final var defaultConverter = new JwtGrantedAuthoritiesConverter();
      final var authorities = new ArrayList<>(defaultConverter.convert(jwt));
      final var realmAccess = jwt.getClaimAsMap("realm_access");
      if (realmAccess != null && realmAccess.containsKey("roles")) {
        @SuppressWarnings("unchecked")
        final var roles = (List<String>) realmAccess.get("roles");
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
      }

      final var cognitoGroups = jwt.getClaimAsStringList("cognito:groups");
      if (cognitoGroups != null) {
        cognitoGroups.forEach(group -> authorities.add(new SimpleGrantedAuthority("ROLE_" + group)));
      }

      return new JwtAuthenticationToken(jwt, authorities);
    };
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
