#!/usr/bin/env bash

# Shared helpers for the chaos scripts. Talks to the Toxiproxy control API.
set -euo pipefail

TOXIPROXY_URL="${TOXIPROXY_URL:-http://localhost:8474}"

toxi() { curl -sS "$@"; }

proxy_enabled() {
  local name="$1" enabled="$2" code
  code=$(toxi -o /dev/null -w "%{http_code}" -X POST "${TOXIPROXY_URL}/proxies/${name}" \
    -H 'Content-Type: application/json' \
    -d "{\"enabled\":${enabled}}")
  [ "$code" -eq 200 ] || { echo "proxy '${name}' toggle failed: HTTP ${code}" >&2; return 1; }
  echo "proxy '${name}' enabled=${enabled}"
}

add_latency() {
  local name="$1" ms="$2" code
  code=$(toxi -o /dev/null -w "%{http_code}" -X POST "${TOXIPROXY_URL}/proxies/${name}/toxics" \
    -H 'Content-Type: application/json' \
    -d "{\"name\":\"${name}-latency\",\"type\":\"latency\",\"attributes\":{\"latency\":${ms}}}")
  [ "$code" -eq 200 ] || { echo "add_latency '${name}' failed: HTTP ${code}" >&2; return 1; }
  echo "proxy '${name}' latency=${ms}ms"
}
