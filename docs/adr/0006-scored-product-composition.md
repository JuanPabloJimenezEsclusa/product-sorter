# ADR 0006: Score as Nullable Field on ProductResponse

**Date:** 2026-06-24

## Context

The sort endpoint returns products with a computed score. The product listing endpoint returns products without a score. Both share the same response schema. We needed to decide how to represent the score in the API contract.

## Decision

`ProductResponse` includes a nullable `score` field (`Double`, `nullable: true`). The same schema is reused across both endpoints — the sort endpoint populates `score`, the listing endpoint omits it or returns `null`.

## Rationale

- **Single schema** — One `ProductResponse` type serves both endpoints; consumers can ignore `score` when it is `null`
- **OpenAPI-native** — `nullable: true` is a standard OpenAPI 3.1 feature; client generators handle it without custom templates
- **Minimal surface** — No wrapper type, no extra nesting in the response payload

## Consequences

- API consumers must check `score != null` before interpreting its value
- Adding score to additional endpoints requires no schema changes
- The contract does not distinguish "no score" from "score of zero"; `0.0` and `null` are different values