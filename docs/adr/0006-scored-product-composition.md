# ADR 0006: ScoredProduct Composition Over Field Duplication

**Date:** 2026-06-24

## Context

The sort endpoint returns scored products: each product has its fields (id, name, salesUnits, stock) plus a computed `score`. We needed to decide whether to flatten the score into the product or compose the product with a score.

## Decision

`ScoredProduct` composes `ProductResponse` rather than duplicating its fields:

```json
{
  "product": { "id": "...", "name": "...", "salesUnits": 100, "stock": [...] },
  "score": 0.87
}
```

## Rationale

- **DRY** — `ProductResponse` is the single schema for product data across all endpoints. Composing it avoids maintaining a parallel schema with all product fields plus `score`
- **Semantic** — Scoring is a projection *over* a product, not a flattened version of it. Composition expresses this relationship explicitly
- **Evolution** — Adding a field to `ProductResponse` (e.g. `category`) automatically enriches the sort response without schema changes

## Consequences

- API consumers must navigate an extra nesting level (`data[0].product.id` instead of `data[0].id`)
- Response payload is slightly larger due to the nested structure (marginal for typical page sizes of 20)
