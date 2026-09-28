# ADR 0011: Trace Correlation Identity and Batched Span Export

**Date:** 2026-09-27

## Context

ADR 0007 promises that logs (Loki) and traces (Tempo) join on `traceId`. They did not. `MdcFilter` ran
first and wrote a random six-character id; `TracingFilter` created the SERVER span and never wrote its
trace id into MDC. Inbound W3C `traceparent` was ignored, so a distributed trace broke at this service.
Spans exported synchronously, making OTLP collector latency a request-latency dependency.

## Decision

- `TracingFilter` at `HIGHEST_PRECEDENCE`, `MdcFilter` at `HIGHEST_PRECEDENCE + 1`, so a current span
  exists before MDC is populated. `traceId` = active span id, else a pre-existing MDC value, else the
  short random fallback.
- The W3C `TextMapPropagator` is set **explicitly** and inbound `traceparent` becomes the server span's
  parent. `OpenTelemetrySdk.builder()` defaults to noop propagators, which silently yields an all-zero
  trace id.
- Spans export in batches, with `@Bean(destroyMethod = "close")` so shutdown flushes.

**Supersedes ADR 0007's UUID-generated `traceId` decision.**

## Consequences

- Exactly one SERVER span per request, from the manual filter — **because Spring Boot 4.1 moved tracing
  auto-configuration into `spring-boot-micrometer-tracing`, which is not on this classpath.** A version
  coincidence, not a guarantee: adding that module reintroduces duplicate spans. `TracingSpanCountTest`
  asserts the count, not merely that a span exists.
- MDC `traceId` is now a 32-hex trace id, not six characters.
- A test inspecting exported spans must force-flush the provider; batch export is not per-request.
