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

echo "[1/5] Starting services..."
"${SCRIPT_DIR}/start.sh"

echo "[2/5] Generating ${COUNT} products..."
"${SCRIPT_DIR}/generate-data.sh" "${COUNT}" "${RESULTS_DIR}/${SESSION}-data.jsonl"

echo "[3/5] Importing into MongoDB..."
"${SCRIPT_DIR}/import-data.sh" "${RESULTS_DIR}/${SESSION}-data.jsonl"

echo "[4/5] Warmup (${WARMUP}s, 5 VUs)..."
docker compose -f docker-compose.yml -f perf/docker-compose.perf.yml --profile perf run --rm \
  -e APP_URL=http://product-sorter-app:8880 \
  -e KEYCLOAK_URL=http://keycloak:8080 \
  k6 run --vus 5 --duration "${WARMUP}s" --quiet --no-thresholds /scripts/mixed-workload.js

echo "[5/5] Running performance test (10→50→100 users)..."
docker compose -f docker-compose.yml -f perf/docker-compose.perf.yml --profile perf run --rm \
  -e APP_URL=http://product-sorter-app:8880 \
  -e KEYCLOAK_URL=http://keycloak:8080 \
  k6 run \
    --quiet --no-thresholds \
    --out json="/results/${SESSION}-raw.json" \
    --summary-export="/results/${SESSION}-summary.json" \
    --summary-trend-stats="avg,min,med,max,p(90),p(95),p(99)" \
    /scripts/mixed-workload.js 2>&1 | tee "$RESULTS_DIR/${SESSION}-console.log"

echo "=== Session suite done. Results in ${RESULTS_DIR}/ ==="
"$SCRIPT_DIR/report.sh"
