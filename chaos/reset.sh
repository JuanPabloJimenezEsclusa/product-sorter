#!/usr/bin/env bash

# Clear all faults: re-enable every proxy and remove all toxics.
set -euo pipefail
cd "$(dirname "$0")"; source ./faults/lib.sh
curl -fsS -X POST "${TOXIPROXY_URL}/reset" >/dev/null
echo "all proxies enabled and toxics cleared"
