# ADR 0008: Pure Hexagonal Adapter Naming Convention

**Date:** 2026-06-24

## Context

The project initially used a mixed naming convention: `adapter-rest` for the inbound HTTP adapter, `infrastructure` for persistence and caching, and `infrastructure-observability` for metrics and logging. While common in Spring Boot projects, this naming is inconsistent with strict hexagonal (ports & adapters) architecture as defined by Alistair Cockburn.

In pure hexagonal architecture, there are only two types of code outside the application core:

| Type | Role |
|------|------|
| **Ports** | Interfaces defining the boundary (inbound/outbound) |
| **Adapters** | Implementations that translate between the core and the outside world |

There is no "infrastructure" layer — everything that is not domain or application logic is an adapter.

## Decision

Rename modules to use a pure `adapter-*` prefix:

| Before | After | Hexagonal role |
|--------|-------|----------------|
| `adapter-rest` | `adapter-rest` (unchanged) | Driving (inbound) adapter — translates HTTP → use case calls |
| `infrastructure` | `adapter-persistence` | Driven (outbound) adapter — translates `ProductRepository` port → MongoDB |
| `infrastructure-observability` | `adapter-observability` | Driving adapter decorator — wraps `SortProductsUseCase` with metrics |

### Sub-package organization within adapters

Driven adapters are further organized by concrete technology:

```
adapter-persistence/
└── src/main/java/dev/jpje/productsorter/adapter/persistence/
    ├── mongo/          ← MongoDB-specific implementation of ProductRepository
    │   ├── MongoProductRepositoryAdapter.java
    │   ├── ProductDocumentMapper.java
    │   └── entity/
    └── cache/          ← Cross-cutting caching (supports multiple adapters)
        ├── MultiTierCache.java
        ├── CompositeCacheManager.java
        └── CacheConfig.java
```

## Rationale

- **Conceptual purity** — Every module outside `domain` and `application` is an adapter. No ambiguity about layer responsibilities
- **Discoverability** — New team members can immediately identify all adapters by the `adapter-*` prefix without reading pom.xml files
- **Extensibility** — Adding a new database (e.g. PostgreSQL) means adding `adapter-persistence/src/main/java/.../persistence/postgres/` without restructuring existing code
- **Testability** — Each adapter is independently testable with its own technology-specific mocks/containers

## Consequences

- Module names changed for 2 out of 3 adapters, requiring pom.xml and import updates
- The adage "there is no infrastructure in hexagonal architecture" must be remembered during code review (no new `infrastructure` modules should be introduced)
- Developers familiar with Spring Boot's layered architecture may initially find the flat adapter structure unfamiliar
