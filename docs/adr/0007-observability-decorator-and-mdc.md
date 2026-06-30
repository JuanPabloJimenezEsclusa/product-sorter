# ADR 0007: Observability — Decorator Pattern and MDC Correlation

**Date:** 2026-06-24

## Context

The application needs observability that cuts across architectural layers: metrics for the sorting use case, distributed tracing, and log correlation. These concerns should not leak into domain or application code.

## Decision

### Decorator pattern for metrics

Use `MetricsSortProductsUseCase` as a decorator that wraps `SortProductsUseCase` transparently. The decorator lives in `adapter-observability` and records Micrometer metrics before/after delegating to the real implementation.

### MDC correlation

Use `MdcFilter`, a Spring `OncePerRequestFilter`, to inject `traceId` (UUID-generated if absent), `requestUri`, and the `X-Request-Id` header into SLF4J MDC. Logs are shipped to Loki via Loki4j appender with `traceId` in the log pattern, correlating logs with Tempo traces.

## Rationale

- **Layer separation** — Metrics are an adapter-observability concern. The decorator wraps the use case without modifying domain or application code, preserving hexagonal architecture boundaries
- **Transparent** — The composition root (`ApplicationConfig`) wires the decorator. The controller and use case implementation are unaware of its existence
- **Zero-code observability** — OpenTelemetry auto-instrumentation handles HTTP traces; Micrometer captures JVM and cache metrics automatically. The decorator only adds business-specific metrics (sorting duration, product counts, weight distribution)
- **Trace correlation** — `traceId` is the common key linking logs (Loki), traces (Tempo), and metrics (Prometheus)

## Consequences

- Every request goes through an additional method call (negligible overhead, ~1µs)
- MDC injection adds a filter to the servlet stack
- Loki must be running for seamless log shipping (graceful degradation if unavailable)
