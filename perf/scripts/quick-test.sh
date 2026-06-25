#!/bin/bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PERF_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
PROJECT_DIR="$(cd "${PERF_DIR}/.." && pwd)"
RESULTS_DIR="${PERF_DIR}/report/results"

mkdir -p "${RESULTS_DIR}"

sessions=(
  "10000:10k-products:15:5"
  "100000:100k-products:15:5"
  "1000000:1M-products:60:10"
)

echo "=== Quick Performance Test Suite ==="
echo "Duration: ~7 min per session (steady=90s, ramp=30s)"
echo

echo "[1/6] Starting services..."
"${SCRIPT_DIR}/start.sh"
echo

for entry in "${sessions[@]}"; do
  IFS=":" read -r count session warmup warmup_vus <<< "$entry"

  echo "--- ${session} (${count} products) ---"

  echo "[2/6] Generating ${count} products..."
  "${SCRIPT_DIR}/generate-data.sh" "${count}" "${RESULTS_DIR}/${session}-data.jsonl"

  echo "[3/6] Importing into MongoDB..."
  "${SCRIPT_DIR}/import-data.sh" "${RESULTS_DIR}/${session}-data.jsonl"

  echo "[4/6] MongoDB cache pre-warm ..."
  mongo_uri="${MONGO_URI:-mongodb://localhost:27017/productsorter}"
  timeout 30 mongosh --quiet "$mongo_uri" --eval "
    db.products.aggregate([{\$sort:{salesUnits:-1}},{\$limit:2000}]).toArray();
    print('Cache warmed');
  " || echo "(pre-warm skipped — timeout or index missing)"

  echo "[5/6] App warmup (${warmup}s, ${warmup_vus} VUs) ..."
  cd "${PROJECT_DIR}"
  docker compose -f docker-compose.yml -f perf/docker-compose.perf.yml --profile perf run --rm \
    -e APP_URL=http://product-sorter-app:8880 \
    -e KEYCLOAK_URL=http://keycloak:8080 \
    k6 run --vus "${warmup_vus}" --duration "${warmup}s" --quiet --no-thresholds /scripts/mixed-workload-quick.js

  echo "[6/6] Testing 10→50→100 users (steady=${STEADY_DURATION:-90s}, ramp=${RAMP_DURATION:-30s})..."
  docker compose -f docker-compose.yml -f perf/docker-compose.perf.yml --profile perf run --rm \
    -e APP_URL=http://product-sorter-app:8880 \
    -e KEYCLOAK_URL=http://keycloak:8080 \
    -e STEADY_DURATION="${STEADY_DURATION:-90s}" \
    -e RAMP_DURATION="${RAMP_DURATION:-30s}" \
    -e WARMUP_DURATION="${warmup}s" \
    k6 run \
      --quiet --no-thresholds \
      --out json="/results/${session}-quick-raw.json" \
      --summary-export="/results/${session}-quick-summary.json" \
      --summary-trend-stats="avg,min,med,max,p(90),p(95),p(99)" \
      /scripts/mixed-workload-quick.js 2>&1 | tee "${RESULTS_DIR}/${session}-quick-console.log"

  echo "${session} complete."
  echo
done

echo "=== Quick suite done. Results in ${RESULTS_DIR}/*-quick-* ==="
"$SCRIPT_DIR/report.sh"
