#!/bin/bash

set -euo pipefail

COUNT=${1:?Usage: run-session.sh <product_count> <session_name> [warmup_seconds]}
SESSION=${2:?}
WARMUP=${3:-30}
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PERF_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
PROJECT_DIR="$(cd "${PERF_DIR}/.." && pwd)"
RESULTS_DIR="${PERF_DIR}/report/results"

mkdir -p "$RESULTS_DIR"

echo "=== Session: $SESSION (${COUNT} products, ${WARMUP}s warmup) ==="

echo "[1/6] Generating ${COUNT} products..."
"${SCRIPT_DIR}/generate-data.sh" "${COUNT}" "${RESULTS_DIR}/${SESSION}-data.jsonl"

echo "[2/6] Importing into MongoDB..."
MONGO_URI="${MONGO_URI:-mongodb://localhost:27017/productsorter}" \
  "${SCRIPT_DIR}/import-data.sh" "${RESULTS_DIR}/${SESSION}-data.jsonl"

echo "[3/6] Starting services..."
"${SCRIPT_DIR}/start.sh"

echo "[4/6] Warmup (${WARMUP}s, 5 VUs)..."
docker compose -f docker-compose.yml -f perf/docker-compose.perf.yml --profile perf run --rm \
  -e APP_URL=http://product-sorter-app:8880 \
  -e KEYCLOAK_URL=http://keycloak:8080 \
  k6 run --vus 5 --duration "${WARMUP}s" --quiet /scripts/mixed-workload.js

echo "[5/6] Running performance test (10→50→100 users)..."
docker compose -f docker-compose.yml -f perf/docker-compose.perf.yml --profile perf run --rm \
  -e APP_URL=http://product-sorter-app:8880 \
  -e KEYCLOAK_URL=http://keycloak:8080 \
  k6 run \
    --out json="/results/${SESSION}-raw.json" \
    --summary-export="/results/${SESSION}-summary.json" \
    --summary-trend-stats="avg,min,med,max,p(90),p(95),p(99)" \
    /scripts/mixed-workload.js 2>&1 | tee "$RESULTS_DIR/${SESSION}-console.log"

echo "[6/6] Collecting Prometheus snapshot..."
curl -s "http://localhost:9090/api/v1/query?query=sorting_duration_seconds{application='product-sorter',quantile='0.95'}" \
  > "${RESULTS_DIR}/${SESSION}-prom-p95.json"
curl -s "http://localhost:9090/api/v1/query?query=rate(sorting_requests_total{application='product-sorter'}[1m])" \
  > "${RESULTS_DIR}/${SESSION}-prom-rps.json"
curl -s "http://localhost:9090/api/v1/query?query=rate(http_server_requests_seconds_count{application='product-sorter'}[1m])" \
  > "${RESULTS_DIR}/${SESSION}-prom-http.json"

echo "Session ${SESSION} complete."
echo "Results in: ${RESULTS_DIR}/${SESSION}-*"
