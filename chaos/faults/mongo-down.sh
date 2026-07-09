#!/usr/bin/env bash

# MongoDB outage -> breaker opens -> 503 (run reset.sh to recover).
set -euo pipefail
cd "$(dirname "$0")"; source ./lib.sh
proxy_enabled mongo false
