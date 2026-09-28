# ADR 0012: Translate Data-Access Failures to Service Unavailable

**Date:** 2026-09-27

## Context

The decorator chain (ADR 0009) translated only `CallNotPermittedException` and `BulkheadFullException`
to `503`. A genuine `DataAccessException` — exhausted retry, timeout, `DataAccessResourceFailureException`
— escaped to `handleGeneral` as **`500`**, contradicting the published contract, which promises `503` for
upstream unavailability. Clients branching on status could not retry a retryable failure.

## Decision

Catch `CallNotPermittedException | BulkheadFullException | DataAccessException` at the single decorator
choke point every catalog read passes through, rethrowing `RepositoryUnavailableException` → `503`. A
non-`DataAccessException` is deliberately **not** caught and remains `500`: the seam is precise because
Spring Data classifies database failures under `DataAccessException`, so it captures "the dependency
failed" without reporting genuine defects as `503`.

## Consequences

- **Supersedes ADR 0009's "timeouts surface as `500`" consequence** — they now return `503`.
- Published change: `500` → `503` for data-access failures. Clients that retry benefit; clients that
  branch on status observe new `503`s.
- Non-data-access faults still return `500`, so the error surface stays discriminating.
- Contract assertions cover the `503` path, so dropping `DataAccessException` from the catch fails the build.
