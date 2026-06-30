# Performance Tests

k6-based load tests for the product-sorter API across three data volumes and three concurrency levels.

## Quick start

```bash
# Smoke test (3 sessions, ~20 min total) — use for CI or rapid feedback
./perf/scripts/quick-test.sh

# Full suite (3 sessions, ~45 min total) — use for detailed reporting
./perf/scripts/run-session.sh 10000  "10k-products"  30
./perf/scripts/run-session.sh 100000 "100k-products" 30
./perf/scripts/run-session.sh 1000000 "1M-products"  60

# Aggregate report (after any run)
./perf/scripts/report.sh

# Cleanup
./perf/scripts/cleanup.sh
```

**Live dashboard:** `http://localhost:3000/d/perf-test` (admin / admin)

## Test matrix

| Products | Users (ramp) | Full (steady) | Quick (steady) | Warmup |
|----------|-------------|---------------|----------------|--------|
| 10,000   | 5 → 10 → 50 → 100 | 3 min | 90s | 15-30s |
| 100,000  | 5 → 10 → 50 → 100 | 3 min | 90s | 15-30s |
| 1,000,000 | 5 → 10 → 50 → 100 | 3 min | 90s | 30-60s |

Quick mode trades statistical stability for speed — p99 may be noisier but p95 is reliable enough for trend detection.

**Workload:** 70% list (`GET /products?size=20`) + 30% sort (`POST /products/sort?size=20`).

Sort requests use random weights each iteration to bypass cache. Think time: 1s.

## Structure

```
perf/
├── docker-compose.perf.yml      # k6 container in product-sorter network
├── k6/
│   ├── lib/
│   │   ├── auth.js              # OAuth2 client_credentials → Keycloak
│   │   └── headers.js           # Bearer token headers
│   ├── list-products.js         # GET /products
│   ├── sort-products.js         # POST /products/sort
│   ├── mixed-workload.js        # 70/30 mixed (12 min, full)
│   └── mixed-workload-quick.js  # 70/30 mixed (6 min, smoke)
├── scripts/
│   ├── generate-data.sh         # ProductDataGenerator → JSONL
│   ├── import-data.sh           # mongoimport --drop
│   ├── start.sh                 # Start app stack + perf container, wait for health
│   ├── quick-test.sh            # Smoke suite: all 3 volumes, 6 min each
│   ├── run-session.sh           # Full session orchestrator (6 steps)
│   ├── cleanup.sh               # Stop services, remove volumes, delete app image
│   └── report.sh                # Markdown report from k6 summaries
└── report/
    └── results/                 # k6 JSON output
```

## Prerequisites

- Docker stack running: `docker compose up -d`
- App built: `mvn package -DskipTests`
- `jq` and `bc` installed (for report.sh)
- `mongoimport` available in PATH (or MongoDB container accessible)
- Keycloak client `product-sorter-client` (secret: `product-sorter-secret`) with service accounts enabled

## Key metrics collected

| Metric | Source |
|--------|--------|
| HTTP p50/p95/p99 per endpoint | k6 + Prometheus |
| Sort duration p50/p95/p99 | Micrometer → Prometheus |
| Request rate (req/s) | k6 |
| JVM heap, GC pauses, virtual threads | Micrometer → Prometheus |
| Fail rate | k6 thresholds |
