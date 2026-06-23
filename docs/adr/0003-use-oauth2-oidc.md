# ADR 0003: Use OAuth 2.0 / OIDC for API Authentication

**Date:** 2026-06-24

## Context

The product sorting API exposes business data that should not be publicly accessible without authentication. The system needed an authentication mechanism that supports:
- Delegated access (machine-to-machine and user-to-machine)
- Standard token format (JWT) with verifiable claims
- External identity provider for production deployments

## Decision

Use OAuth 2.0 with OpenID Connect (OIDC) for API authentication, with Bearer JWT tokens validated by the resource server.

## Rationale

- **Standard protocol** — OAuth 2.0 is the industry standard for API security; OIDC adds identity layer on top
- **JWT tokens** — Stateless verification; no token introspection endpoint needed per request
- **Resource server pattern** — Spring Boot's `oauth2-resource-server` starter handles token validation with minimal configuration
- **Keycloak** — Included via Docker Compose for local development; can be replaced with any OIDC provider (Auth0, Okta, Azure AD) in production
- **E2E tests** — JWT generation is done in-test with RSA key pairs; no external IdP dependency for testing

## Consequences

- Every API request must include a valid Bearer JWT (401 otherwise)
- Token expiry requires client-side refresh logic
- Keycloak container adds ~2s to Docker Compose startup
