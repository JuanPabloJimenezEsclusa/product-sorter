# ADR 0009: Resilience — Resilience4j Decorators and Timeout Strategy

**Date:** 2026-06-24

## Context

The service has three outbound integration points that can fail or slow down: MongoDB (primary store), Redis (L2 cache), and the OAuth2 JWKS endpoint. Without fault handling, a slow or unavailable dependency cascades into hung threads and failed requests. Resilience must not leak into domain or application code, and must not distort performance tests: load tests showed that an aggressive fault layer can *create* failures (bulkhead rejections, retry amplification, circuit-breaker flapping) rather than absorb them.

## Decision

Use **Resilience4j core modules wired programmatically** (no Spring Boot starter) and applied as **decorators** at each outbound boundary, keeping domain/application framework-free.

| Boundary | Strategy | Degradation |
|----------|----------|-------------|
| MongoDB (`ResilientProductRepository`) | Bulkhead → CircuitBreaker → Retry | Open/full → fail fast `503` |
| Redis L2 (`ResilientCache`) | CircuitBreaker | Open → cache miss (fall back to Mongo) |
| JWKS fetch (`JwtDecoderResilienceConfig`) | Retry + connect/read timeouts | Transient fetch retried |
| REST ingress (`RateLimitInterceptor`) | RateLimiter | Over limit → `429` |

Key parameters:

- **Composition order** `Bulkhead → CircuitBreaker → Retry` (retry innermost): a burst of transient retries counts as one circuit-breaker call, so the breaker trips only after retries are exhausted.
- **Retry connectivity/failover only, never timeouts** (`MongoFaultClassifier`): retries socket/`NotPrimary`/`NodeIsRecovering` faults with exponential backoff + jitter; a timed-out call already spent its budget, so retrying only amplifies load.
- **Bulkhead sized above concurrency, below the pool**: 128 concurrent (≥ expected in-flight, ≪ Mongo `maxPoolSize=500`) — protects the DB without throttling legitimate load.
- **Time-based, slow-call-aware circuit breaker**: 30 s window with a minimum call count avoids flapping; calls ≥6 s count as slow, distinguishing "slow" from "failed".

## Rationale

- **Boundary-appropriate behavior** — a cache outage should degrade silently (Redis bypass), while a store outage should fail fast rather than hang; one-size resilience would do neither well.
- **Version independence** — core modules avoid coupling to a Boot-version-specific autoconfiguration starter.
- **Capacity tests stay honest** — with these values the fault layer stays dormant under legitimate load (≤100 VUs); observed `sort` failures at scale are genuine Mongo operation timeouts (`500`), not resilience artifacts.
- **Observable** — circuit-breaker, retry, bulkhead, and rate-limiter states are exported via Micrometer to Prometheus/Grafana.

## Consequences

- Each cache-miss repository call passes through three decorators (negligible overhead).
- Redis resilience lives in the cache layer, not the repository decorator, because Redis I/O happens *inside* the `@Cacheable` proxy.
- Resilience is validated end-to-end by chaos tests (Testcontainers + Toxiproxy + MockWebServer) covering fail-fast, graceful degradation and recovery, plus a manual Toxiproxy runbook against the Docker stack (see [chaos/](../../chaos/README.md)).
