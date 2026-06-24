# ADR 0002: Use Multi-Tier Cache (Caffeine L1 + Redis L2)

**Date:** 2026-06-24

## Context

Sorting 250k+ products involves two MongoDB queries (`findAllScoreable`, `findMaxSalesUnits`) that are read-heavy and compute-intensive. The same catalog data is sorted repeatedly with different weight combinations. Caching is required to reduce database load and latency.

## Decision

Use a two-tier cache pattern with a `CompositeCacheManager` wrapping both `CaffeineCacheManager` (L1) and `RedisCacheManager` (L2). When both managers declare the same cache name, a `MultiTierCache` is created.

### Cache configuration

- **L1:** Caffeine (in-process, 30s TTL, max 100 entries)
- **L2:** Redis (external, 120s TTL)

### Read/Write behaviour

- **Reads:** L1 → miss → L2 → miss → null. Populates L1 on L2 hit.
- **Writes:** writes to both L1 and L2.
- **Evictions:** evicts from both tiers.

## Rationale

- **L1 (Caffeine)** — Eliminates serialization overhead for hot keys; 30s TTL keeps cache fresh without manual invalidation
- **L2 (Redis)** — Shared across instances; survives restarts; 120s TTL balances freshness with cache-hit ratio
- **Read-through** — L1 → L2 → MongoDB cascade, populating L1 on L2 hit and L2 on DB hit
- **Cache-aside** — Application controls population; no write-through complexity
- **Spring Cache abstraction** — Swappable without changing business code (`@Cacheable` annotations)

## Consequences

- Cache consistency is best-effort (TTL-based, no active invalidation for updates)
- Two network hops on cold start (Redis fetch before DB)
- Redis dependency adds operational complexity (monitoring, persistence config)
