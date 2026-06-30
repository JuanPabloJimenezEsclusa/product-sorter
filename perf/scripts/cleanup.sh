#!/bin/bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

echo "=== Stopping all services ==="
cd "${PROJECT_DIR}"
docker compose \
  -f docker-compose.yml \
  -f perf/docker-compose.perf.yml \
  --profile app --profile perf \
  down --remove-orphans -v 2>/dev/null || true

echo "=== Removing app image ==="
docker rmi jpje/product-sorter-app:1.0.0 2>/dev/null || true

echo "=== Prune docker service ==="
docker system prune --force

echo "=== Cleanup complete ==="
