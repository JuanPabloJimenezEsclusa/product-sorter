# ADR 0012: Translate Data-Access Failures to Service Unavailable

**Date:** 2026-09-27

## Context

ADR 0009 established the MongoDB decorator chain (bulkhead → circuit breaker → retry) and recorded that an open circuit or a full bulkhead fails fast as `503`, while genuine load-time Mongo operation timeouts surface as `500`. The published API contract, however, states that upstream unavailability is `503 SERVICE_UNAVAILABLE`.

Only `CallNotPermittedException` and `BulkheadFullException` were translated to `RepositoryUnavailableException`. A genuine `DataAccessException` — an exhausted-retry failure, a query or execution timeout, or a `DataAccessResourceFailureException` — was not caught by the decorator, escaped to `GlobalExceptionHandler.handleGeneral`, and was returned as `500 INTERNAL_ERROR`. The contract therefore under-reported a retryable dependency failure as a server bug, and clients that branch on status code could not retry it.

## Decision

Translate data-access failures to `RepositoryUnavailableException` at the single decorator choke point that every catalog read passes through. `ResilientProductRepository.call` catches `CallNotPermittedException | BulkheadFullException | DataAccessException` and rethrows `RepositoryUnavailableException`, which the REST layer already maps to `503 SERVICE_UNAVAILABLE` with a stable client-facing message. A failure that is **not** a `DataAccessException` is deliberately left uncaught and still propagates unchanged to `500 INTERNAL_ERROR`.

**Note (F15, internal, same file):** the decorator chain is now composed once in the constructor over a thread-confined per-invocation action holder instead of being rebuilt on every call. Chain order is unchanged — bulkhead outermost, then circuit breaker, then retry innermost — and this refactor has no observable effect; it is recorded here only because it shares the file with this decision.

## Rationale

- **Translate at the choke point, not per caller** — every catalog read funnels through `ResilientProductRepository`, so one catch covers retry exhaustion, non-retried timeouts, and any future data-access failure without duplicating translation logic in callers.
- **`DataAccessException` is the right seam** — Spring Data classifies database failures under `DataAccessException`, so the catch is precise: it captures "the dependency failed" without swallowing unrelated programming errors.
- **Do not widen the catch** — catching broader than `DataAccessException` (for example `RuntimeException`) would report genuine defects as `503` and hide them from operators; a non-data-access failure must remain a `500` so it stays visible as a bug.
- **Honour the published contract** — the contract already promises `503` for upstream unavailability; the fix makes the implementation match the contract rather than inventing a new status.

## Consequences

- **This supersedes ADR 0009's "observed `sort` failures at scale are genuine Mongo operation timeouts (`500`)" consequence.** Those Timeouts now surface as `503 SERVICE_UNAVAILABLE`.
- Published behaviour change: a database failure that previously returned `500 INTERNAL_ERROR` now returns `503 SERVICE_UNAVAILABLE`. Clients that treat all `5xx` alike are unaffected; clients that branch on status observe new `503`s.
- Genuine non-data-access faults still return `500 INTERNAL_ERROR`, so the error surface remains discriminating.
- The `503` path is now covered by contract assertions (exhausted-retry and query-timeout cases) so a future regression that drops `DataAccessException` from the catch fails the build.
