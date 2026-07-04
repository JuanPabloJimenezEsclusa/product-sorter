# ADR 0001: Use MongoDB as Primary Database

**Date:** 2026-06-24

## Context

The product catalog stores products with embedded stock entries per size (a product has N `StockBySize` entries). The two core use cases are paginated product listing (`findPage`) and weighted product sorting (`sortByWeights`). The sorting use case computes a score from `salesUnits` and `stockRatio` using user-supplied weights and returns results in descending score order.

We also need an identifier strategy: identifiers must support lexicographic cursor-based pagination via `_id`.

## Decision

Use MongoDB as the database with plain `String` document identifiers.

The adapter accesses MongoDB through `MongoTemplate` (Spring Data MongoDB's low-level operations API) rather than `MongoRepository` (Spring Data's repository abstraction). This keeps `ProductRepository` as the single domain-facing interface defined in the domain layer, with the adapter implementing it directly.

## Rationale

### MongoDB

- **Document model** — Product with embedded `StockBySize[]` maps directly to a MongoDB document, avoiding JOINs or ORM mappings
- **Aggregation pipeline** — Weighted score computation (`$addFields`), and sorting (`$sort`), execute entirely within MongoDB, avoiding in-memory scoring of large result sets
- **Cursor-based pagination** — `_id`-based cursor provides stable pagination without offset drift
- **Schema simplicity** — The `ProductDocument` record maps to the `products` collection using only `@Document` and `@Id` annotations; no custom converters
- **Testcontainers** — MongoDB container starts in under 1s for integration tests

### Plain string identifiers

- **Simplicity** — `@Id String id` maps directly to `_id` with no type conversion
- **Compatibility** — External systems or data generators can insert documents without binary format concerns
- **Cursor stability** — String `_id` provides lexicographic ordering for cursor-based pagination

### MongoTemplate over MongoRepository

- **Domain-first repository** — `ProductRepository` is defined in the domain module as the single contract; `MongoTemplate` is a private implementation detail of the adapter
- **Aggregation control** — `sortByWeights` requires multi-stage aggregation pipelines (`$addFields`, `$sort`) that `MongoTemplate.aggregate()` supports directly without annotation-driven query derivation
- **No leaky abstractions** — `MongoRepository` would force Spring Data interfaces (`MongoRepository<ProductDocument, String>`) into our own adapter, blurring the boundary between the domain port and the persistence technology

## Consequences

- The `products` collection relies on the default `_id` index only; no additional indices are created at startup
- The weighted score computation is coupled to MongoDB's aggregation pipeline (`ProductSorterHelper`); alternative database backends would require reimplementing the scoring in the query layer
- `ProductId` is a plain `String` wrapper with no format validation — any string value is accepted as a valid identifier
- No transactions are used; the application is read-only so multi-document consistency is not required
- Spring Boot autoconfiguration manages the `MongoTemplate` bean via `application.yml`; no manual client setup
