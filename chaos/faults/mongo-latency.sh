#!/usr/bin/env bash

# MongoDB latency beyond the operation timeout -> sort times out (500),
# then the circuit breaker opens -> fail fast (503).
set -euo pipefail
cd "$(dirname "$0")"; source ./lib.sh
add_latency mongo "${1:-6000}"
