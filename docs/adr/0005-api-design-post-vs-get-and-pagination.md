# ADR 0005: API Design — POST for Sorting and Cursor-Based Pagination

**Date:** 2026-06-24

## Context

The API exposes two operations: product listing (`GET /products`) and weighted product sorting (`POST /products/sort`). The sort operation accepts a `weights` object (e.g. `{"salesUnits": 0.7, "stockRatio": 0.3}`) as structured input. Both endpoints need pagination to handle catalog sizes beyond what fits in a single response.

## Decision

### POST for sorting

`POST /products/sort` uses POST rather than GET because the weights map is structured data that belongs in the request body, not query parameters.

### Cursor-based pagination

Both endpoints use `cursor` (opaque string) and `size` (1–100, default 20) as query parameters. The response includes `data`, `size`, and `nextCursor`. `nextCursor` is `null` when there are no more pages — no `total` count is tracked.

## Rationale

### POST vs GET

- **Structured input** — Encoding nested weight objects as query parameters is fragile and does not scale as new sorting criteria are added
- **Body semantics** — POST with a JSON body is the standard pattern for operations that accept complex structured data
- **Cache implications** — POST responses are not cached by HTTP intermediaries; application-level caching handles this in the persistence adapter

### Cursor-based pagination

- **Stability under mutation** — Cursors are resilient to insertions and deletions between page requests; unlike offset pagination, new items don't shift the window
- **No `skip()` performance penalty** — MongoDB's `skip()` degrades linearly with offset; cursor-based pagination uses indexed `_id` traversal at constant cost
- **Opaque cursors** — `nextCursor` is an encoded string that hides implementation details (`score:id` for sort, `0:id` for list) from API consumers

## Consequences

- Sorting weights must be sent in the request body; empty or missing body returns 400
- POST verb prevents URL-based caching by HTTP intermediaries; `@Cacheable` on the repository adapter compensates for first-page requests
- The sort endpoint uses MongoDB aggregation with top-K optimization (`$sort` + `$limit`), which enables a heap-based sort of O(N log K) instead of a full collection sort
- Cursor format is stable but opaque — clients cannot decode or construct cursors independently
