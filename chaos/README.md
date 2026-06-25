# Chaos / Resilience Testing

Two complementary ways to validate the resilience layer:

- **Automated** — `mvn -Pchaos -pl bootstrap test` runs `@Tag("chaos")` tests
  (`DataStoreChaosTest`, `JwksChaosTest`) that spin up MongoDB, Redis and a
  Toxiproxy (plus a MockWebServer JWKS) via Testcontainers and assert
  fail-fast, graceful degradation and recovery. Excluded from the default
  `mvn verify`.
- **Manual** — inject faults against the running Docker stack through Toxiproxy
  and watch the Grafana resilience panels.

## Manual runbook

```bash
docker compose -f docker-compose.yml -f ./chaos/docker-compose.chaos.yml \
  --profile app up -d --build --force-recreate --wait

# Inject a fault, then exercise the API (sort is uncached -> hits MongoDB)
./chaos/faults/mongo-latency.sh
./chaos/faults/mongo-down.sh
./chaos/faults/redis-down.sh
./chaos/faults/redis-flap.sh
./chaos/faults/jwks-down.sh

./chaos/reset.sh
```

## Scenarios and expected behavior

| Script | Fault | Expected API behaviour |
|--------|-------|------------------------|
| `mongo-latency.sh` | MongoDB latency > `timeoutMS` | `sort` times out (`500`); breaker opens → fail fast (`503`) |
| `mongo-down.sh` | MongoDB unreachable | breaker opens → `503`; `reset.sh` → recovers to `200` |
| `redis-down.sh` | Redis unreachable | L2 degrades to a miss; served from MongoDB (`200`) |
| `redis-flap.sh` | Redis toggling | stays available (`200`) as the breaker opens/closes |
| `jwks-down.sh` | JWKS endpoint down | cached keys keep auth working; cold fetch retries then `401` |

Watch in Grafana (`product-sorter` dashboard): **Circuit Breaker State**,
**Retry Calls**, **Rate Limiter Available Permissions**, **Bulkhead Available
Concurrent Calls**. Override the Toxiproxy control URL with `TOXIPROXY_URL`.
