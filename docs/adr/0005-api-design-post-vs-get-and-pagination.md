# ADR 0005: API Design — POST vs GET and Offset Pagination

**Date:** 2026-06-24

## Context

The product sorting API exposes two operations: sorting products by weighted criteria and listing products. We needed to choose the correct HTTP verb for sorting and the pagination strategy for both endpoints.

## Decision

### POST for sorting

`POST /api/v1/products/sort` uses POST rather than GET because sorting is a computational operation (scoring + ordering), not a resource retrieval.

### Offset-based pagination

Use offset-based pagination with `page` (1-indexed, default 1) and `size` (max 100). Omit `total` and `totalPages` from responses.

## Rationale

### POST vs GET

- **Semantic** — Sorting computes a new ordering based on weights; it is not fetching an existing resource. POST is the correct verb for computational operations
- **Request body** — The weights map is complex structured data. Encoding it as query parameters would be fragile (`?salesUnits=0.7&stockRatio=0.3`) and would not scale as new criteria are added
- **Idempotency** — Not required; the same weights may produce different results as product data changes

### Pagination

- **Product stability** — Products are stable during navigation (no insertions or deletions mid-session), so offset-based pagination is safe and simpler than cursor-based
- **No COUNT query** — Omitting `total`/`totalPages` avoids the MongoDB `COLLSCAN` needed for count aggregation. The client detects the end of the result set when the returned page has fewer items than `size`
- **Simplicity** — Offset pagination is intuitive for API consumers: `page=1&size=20` is self-explanatory

## Consequences

- Sorting cannot be cached by URL alone (POST requests are not cached by HTTP intermediaries)
- Large offset values (e.g., page 1000) become progressively slower on MongoDB due to `skip()` performance
- Clients must handle pagination state manually (no `total` field for progress bars)
