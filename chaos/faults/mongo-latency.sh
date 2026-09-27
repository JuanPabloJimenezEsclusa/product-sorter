#!/usr/bin/env bash

# MongoDB latency beyond the operation timeout -> sort times out and the
# translated data-access failure returns 503; the breaker then opens -> fail fast (503).
set -euo pipefail
cd "$(dirname "$0")"; source ./lib.sh
add_latency mongo "${1:-6000}"
