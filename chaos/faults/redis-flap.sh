#!/usr/bin/env bash

# Redis flapping -> breaker opens/closes; requests stay available (200) throughout.
set -euo pipefail
cd "$(dirname "$0")"; source ./lib.sh
for _ in 1 2 3; do
  proxy_enabled redis false; sleep 3
  proxy_enabled redis true;  sleep 3
done
