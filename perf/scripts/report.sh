#!/bin/bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PERF_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
RESULTS_DIR="${PERF_DIR}/report/results"

report_md() {
  echo "# Performance Test Report"  
  echo "**Date:** $(date -u +"%Y-%m-%dT%H:%M:%SZ")"
  echo "**Workload:** mixed (70% list GET + 30% sort POST)"
  echo

  local sessions=()
  while IFS=' ' read -r _ n; do
    sessions+=("$n")
  done < <(for f in "$RESULTS_DIR"/*-summary.json; do
    [ -f "$f" ] || continue
    local name=$(basename "$f" -summary.json)
    local count=0
    [[ "$name" =~ ^([0-9]+)[kK] ]] && count=$((BASH_REMATCH[1] * 1000))
    [[ "$name" =~ ^([0-9]+)M ]] && count=$((BASH_REMATCH[1] * 1000000))
    printf '%010d %s\n' "$count" "$name"
  done | sort -n)

  if [ ${#sessions[@]} -eq 0 ]; then
    echo "No summary files found in $RESULTS_DIR"
    return
  fi

  echo "## Overall Results"
  echo
  echo "| Session  | Total Req | Fail Rate | p50 (ms) | p95 (ms) | p99 (ms) | Max (ms) | Req/s | VUs |"
  echo "|----------|-----------|-----------|----------|----------|----------|----------|-------|-----|"
  for session in "${sessions[@]}"; do
    local s="$RESULTS_DIR/${session}-summary.json"
    local reqs=$(jq -r '.metrics.http_reqs.count // "N/A"' "$s")
    local fails=$(jq -r '.metrics.checks.fails // 0' "$s")
    local fail_pct="–"
    if [ "$reqs" != "N/A" ] && [ "$reqs" != "0" ]; then
      fail_pct=$(( fails * 100 / reqs ))
    fi
    local p50=$(jq -r '.metrics.http_req_duration.med | round' "$s" 2>/dev/null || echo "–")
    local p95=$(jq -r '.metrics.http_req_duration["p(95)"] | round' "$s" 2>/dev/null || echo "–")
    local p99=$(jq -r '.metrics.http_req_duration["p(99)"] | round' "$s" 2>/dev/null || echo "–")
    local maxd=$(jq -r '.metrics.http_req_duration.max | round' "$s" 2>/dev/null || echo "–")
    local rps=$(jq -r '(.metrics.http_reqs.rate * 10 | round) / 10' "$s" 2>/dev/null || echo "–")
    local vus=$(jq -r '.metrics.vus_max.value // .metrics.vus_max.max // "–"' "$s")
    echo "| $session | $reqs | ${fail_pct}% | $p50 | $p95 | $p99 | $maxd | $rps | $vus |"
  done

  echo
  echo "## Per-Endpoint Latency"
  echo
  echo "| Session  | Endpoint | p50 (ms) | p95 (ms) | p99 (ms) | Avg (ms) | Max (ms) |"
  echo "|----------|----------|----------|----------|----------|----------|----------|"
  for session in "${sessions[@]}"; do
    local s="$RESULTS_DIR/${session}-summary.json"
    for ep in list sort; do
      local key="http_req_duration{endpoint:$ep}"
      local p50=$(jq -r ".metrics.\"$key\".med | round" "$s" 2>/dev/null || echo "–")
      local p95=$(jq -r ".metrics.\"$key\".\"p(95)\" | round" "$s" 2>/dev/null || echo "–")
      local p99=$(jq -r ".metrics.\"$key\".\"p(99)\" | round" "$s" 2>/dev/null || echo "–")
      local avg=$(jq -r "(.metrics.\"$key\".avg * 10 | round) / 10" "$s" 2>/dev/null || echo "–")
      local maxd=$(jq -r ".metrics.\"$key\".max | round" "$s" 2>/dev/null || echo "–")
      local name="$session $ep"
      if [ "$p50" = "–" ] && [ "$p95" = "–" ]; then continue; fi
      echo "| $session | $ep | $p50 | $p95 | $p99 | $avg | $maxd |"
    done
  done

  echo
  echo "## Checks"
  echo
  echo "| Session  | Metric | Status | Detail |"
  echo "|----------|--------|--------|--------|"
  for session in "${sessions[@]}"; do
    local s="$RESULTS_DIR/${session}-summary.json"
    local checks=$(jq -c '.root_group.checks // {}' "$s")

    local keys=$(jq -r 'keys[]' <<<"$checks")
    while IFS= read -r key; do
      [ -z "$key" ] && continue
      local passes=$(jq -r ".\"$key\".passes // 0" <<<"$checks")
      local fails=$(jq -r ".\"$key\".fails // 0" <<<"$checks")
      local status="PASS"
      [ "$fails" -gt 0 ] && status="FAIL"
      echo "| $session | check: $key | $status | $passes/$((passes + fails)) |"
    done <<<"$keys"
  done
}

report_md > "${PERF_DIR}/report/report.md"
echo "Report written to ${PERF_DIR}/report/report.md"
