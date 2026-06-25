# ADR 0008: Hexagonal Adapter Naming Convention

**Date:** 2026-06-24

## Context

Hexagonal architecture (ports & adapters) distinguishes two types of code outside the application core: ports (interfaces defining the boundary) and adapters (implementations that translate between the core and the outside world). Module naming must make these roles immediately visible.

Spring Boot projects conventionally use layered names like `infrastructure`, `persistence`, or `web`. These names hide the hexagonal role of each module.

## Decision

Name every module outside `domain` and `application` with the `adapter-*` prefix, matching its hexagonal role:

| Module | Hexagonal role |
|--------|----------------|
| `adapter-rest` | Driving (inbound) adapter — translates HTTP requests into use case calls |
| `adapter-persistence` | Driven (outbound) adapter — implements `ProductRepository` port with MongoDB and caching |
| `adapter-observability` | Driving adapter decorator — wraps `SortProducts` with Micrometer metrics and MDC tracing |

### Sub-package organization within adapters

Driven adapters group implementation by concrete technology:

```
adapter-persistence/
└── src/main/java/dev/jpje/productsorter/adapter/persistence/
    ├── mongo/              ← MongoDB ProductRepository implementation
    │   ├── MongoProductRepositoryAdapter.java
    │   ├── ProductDocumentMapper.java
    │   └── entity/
    └── cache/              ← Cross-cutting caching (supports any persistence adapter)
        ├── MultiTierCache.java
        ├── CompositeCacheManager.java
        └── CacheConfig.java
```

## Rationale

- **Role visibility** — The `adapter-` prefix communicates hexagonal role at the module level without reading POM or source files
- **Flat structure** — All adapters sit at the same level; no nested distinction between "infrastructure" and "adapters"
- **Extensibility** — A new database backend (e.g. `adapter-persistence/.../postgres/`) fits within the existing module without restructuring
- **Test isolation** — Each adapter is independently testable with its own technology-specific containers or mocks

## Consequences

- Team members must distinguish between hexagonal adapters (this convention) and Spring-style `@Adapter` stereotypes — the two are unrelated
- New modules that touch external systems must follow the `adapter-*` prefix; no `infrastructure` or `persistence` module names are permitted
