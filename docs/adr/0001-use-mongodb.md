# ADR 0001: Use MongoDB as Primary Database

**Date:** 2026-06-24

## Context

Product catalog data has a variable schema (new criteria may be added over time). The sorting use case requires fast projection queries (`findAllScoreable` fetches only 3 fields) and paginated access. The data is document-oriented: products have embedded stock entries per size.

Additionally, we need a strategy for document identifiers that works across systems and allows client-side generation.

## Decision

Use MongoDB as the primary database store with UUID strings as document identifiers.

## Rationale

### MongoDB

- **Document model** — Product with nested stock entries maps naturally to a MongoDB document, avoiding JOINs or complex ORM mappings
- **Projection queries** — `findAllScoreable()` with field projection (`_id`, `salesUnits`, `stock`) returns only the data needed for scoring, reducing data transfer by ~80%
- **Index support** — `{salesUnits: -1}` index makes `findMaxSalesUnits()` an IXSCAN (~1ms) instead of COLLSCAN
- **Schema flexibility** — new criteria fields can be added without migrations
- **Testcontainers** — MongoDB 8 community server starts in <1s for integration tests

### UUID as `_id`

- **Global uniqueness** — IDs are unique across systems without a central coordinator
- **Client-side generation** — Clients can generate IDs offline (useful for event-driven or offline-capable architectures)
- **No enumeration** — Avoids sequential ID enumeration by external consumers
- **String storage** — Stored as plain strings, not `BinData` UUID, so Spring Data MongoDB maps directly to `String` without custom converters

## Consequences

- Transactions are not used (the write path is single-document, no跨-document consistency needed)
- MongoDB must be running for the application to function
- No SQL-based analytics or reporting — all reporting goes through the REST API
- UUIDs are less storage-efficient than auto-incrementing integers (36 bytes per string vs 4-8 bytes per int)
- Human readability is reduced compared to sequential IDs
