# ADR 0004: Use Virtual Threads (Project Loom)

**Date:** 2026-06-24

## Context

The application handles blocking I/O operations for each request: MongoDB queries, Redis cache lookups, and HTTP calls to the OIDC provider for JWK set resolution. With platform threads, each concurrent request consumes an OS thread, limiting throughput under load.

## Decision

Use Java 25 virtual threads (Project Loom) enabled by Spring Boot 4.1's virtual thread support.

## Rationale

- **Java 25** — Target runtime with mature virtual thread implementation
- **Spring Boot 4.1** — `spring.threads.virtual.enabled=true` configures Tomcat and all `@Async` executors to use virtual threads automatically
- **Blocking I/O** — Virtual threads handle blocking operations efficiently by yielding during I/O wait, allowing high concurrency without thread pool tuning
- **No code changes** — Framework handles thread management transparently; existing synchronous code just works
- **Testcontainers** — Virtual threads are fully compatible with Testcontainers' blocking API calls

## Consequences

- Requires JDK 25+
- Pinned thread scenarios (synchronized blocks, native frames) can still block carrier threads
- Monitoring must track virtual thread metrics, not just OS thread count
