#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PERF_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
PROJECT_DIR="$(cd "${PERF_DIR}/.." && pwd)"

cd "${PROJECT_DIR}"

if curl -sf http://localhost:8880/actuator/health > /dev/null 2>&1; then
  echo "App stack already running."
else
  echo "Starting app stack..."
  docker compose --profile app up -d --build --wait

  echo "Waiting for app health..."
  for i in $(seq 1 60); do
    if curl -sf http://localhost:8880/actuator/health > /dev/null 2>&1; then
      echo "App healthy."
      break
    fi
    sleep 1
  done

  echo "Waiting for Keycloak..."
  for i in $(seq 1 60); do
    if curl -sf http://localhost:8081/realms/product-sorter/.well-known/openid-configuration > /dev/null 2>&1; then
      echo "Keycloak ready."
      break
    fi
    sleep 1
  done
fi

echo "All services up."
