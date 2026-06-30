# Performance Test Report
**Date:** 2026-06-29T16:19:26Z
**Workload:** mixed (70% list GET + 30% sort POST)

## Overall Results

| Session  | Total Req | Fail Rate | p50 (ms) | p95 (ms) | p99 (ms) | Max (ms) | Req/s | VUs |
|----------|-----------|-----------|----------|----------|----------|----------|-------|-----|
| 10k-products-quick | 18117 | 0% | 3 | 6 | 15 | 118 | 50.2 | 100 |
| 100k-products-quick | 9241 | 1% | 2 | 5 | 60001 | 60006 | 23.7 | 100 |
| 1M-products-quick | 1560 | 17% | 3 | 60001 | 60001 | 60004 | 3.2 | 100 |

## Per-Endpoint Latency

| Session  | Endpoint | p50 (ms) | p95 (ms) | p99 (ms) | Avg (ms) | Max (ms) |
|----------|----------|----------|----------|----------|----------|----------|
| 10k-products-quick | sort | 3 | 7 | 94 | 4.8 | 118 |
| 100k-products-quick | sort | 3 | 60000 | 60001 | 3191.5 | 60006 |
| 1M-products-quick | sort | 60000 | 60001 | 60001 | 31898.5 | 60003 |

## Checks

| Session  | Metric | Status | Detail |
|----------|--------|--------|--------|
| 10k-products-quick | check: list 200 | PASS | 12574/12574 |
| 10k-products-quick | check: sort 200 | PASS | 5443/5443 |
| 100k-products-quick | check: list 200 | PASS | 6368/6368 |
| 100k-products-quick | check: sort 200 | FAIL | 2627/2773 |
| 1M-products-quick | check: list 200 | FAIL | 983/1043 |
| 1M-products-quick | check: sort 200 | FAIL | 201/417 |
