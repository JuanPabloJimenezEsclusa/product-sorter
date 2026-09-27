# ADR 0011: Trace Correlation Identity and Batched Span Export

**Date:** 2026-09-27

## Context

The service ships logs to Loki and traces to Tempo, and treats `traceId` as the join key between them (ADR 0007). The two identifiers were produced by unrelated components and could not be joined:

- `MdcFilter` ran at `HIGHEST_PRECEDENCE` — before any server span existed — and wrote a short random `UUID` into the MDC `traceId` key.
- `TracingFilter` created the OpenTelemetry SERVER span but never wrote its trace id into MDC.

A log line and the trace for the same request therefore carried different, unrelated identifiers. An inbound W3C `traceparent` was also ignored, so the service started a fresh trace instead of continuing the caller's, breaking propagation across services. Finally, spans were exported with a synchronous `SimpleSpanProcessor`, so every completed span triggered its own export request and a slow or unavailable OTLP collector added its latency to request handling.

## Decision

### Filter ordering and trace-id identity

`TracingFilter` runs at `@Order(Ordered.HIGHEST_PRECEDENCE)` and `MdcFilter` at `@Order(Ordered.HIGHEST_PRECEDENCE + 1)`, so the server span exists and is current on the request thread before MDC is populated. `MdcFilter` derives `traceId` from the active span's trace id when the span context is valid, falls back to a pre-existing MDC `traceId` when present, and only then to the short random id. When tracing is disabled no span exists and the random fallback still guarantees a non-blank `traceId`.

### Continuation of upstream trace context

`TracingFilter` extracts the inbound W3C `traceparent` with a `TextMapGetter` and sets the extracted context as the parent of the server span, so an incoming trace is continued rather than replaced. `TracingConfig` sets the W3C `TextMapPropagator` explicitly on the SDK: `OpenTelemetrySdk.builder()` defaults to NOOP propagators, and leaving the default silently yields an all-zero trace id on extraction.

### Batched span export

Spans are exported with a `BatchSpanProcessor` over the OTLP HTTP exporter, and the SDK bean is declared `@Bean(destroyMethod = "close")` so shutdown flushes buffered spans. This decouples collector latency from request latency.

## Rationale

- **A single correlation key** — MDC `traceId` must be the active trace's id, otherwise the Loki/Tempo join ADR 0007 promises does not exist. Ordering the span producer before the MDC consumer is the minimal way to guarantee a valid span context at MDC-population time.
- **Continue, do not restart** — Distributed traces are only useful across service boundaries if an upstream context is honoured; extraction plus an explicit propagator is what makes continuation real.
- **Export off the request path** — A synchronous per-span export made collector availability a request-latency dependency; batching amortises export cost and isolates request handling from collector health.

### Duplicate SERVER spans — measured, hypothesis rejected

It was predicted that `micrometer-tracing-bridge-otel` plus `spring-boot-starter-actuator` on the classpath would auto-configure a second SERVER span alongside the manual `TracingFilter`. Measurement with a recording `SpanExporter` disproved this: **exactly one SERVER span is emitted per request**, from the manual filter. The reason is that Spring Boot 4.1 relocated tracing auto-configuration into the `spring-boot-micrometer-tracing` module, which is **not on this project's dependency set**; consequently there is no Micrometer `Tracer` bean and no `ServerHttpObservationFilter` producing a second span. (Verified against the resolved dependency tree: `spring-boot-micrometer-tracing` is absent; `micrometer-tracing-bridge-otel:1.7.1` is present.)

## Consequences

- The manual filter coexists with the framework only because the auto-configuration module is absent. This is a version coincidence, not a guarantee: adding `spring-boot-micrometer-tracing` (or any other second SERVER-span source) would silently reintroduce duplicate spans. `TracingSpanCountTest` exists to fail the build if that happens, which is why it asserts the span count rather than merely that a span exists.
- The MDC `traceId` is now the 32-hex-character trace id when a span is active, not the previous 6-character random value; log-pattern width and any downstream `traceId` parsing must tolerate the longer value.
- With batched export, spans are not exported individually at request end; a test that inspects exported spans must force-flush the provider before asserting (as the recording test does).
- When tracing is disabled (`otel.enabled=false`), the random-fallback `traceId` behaviour is unchanged.
