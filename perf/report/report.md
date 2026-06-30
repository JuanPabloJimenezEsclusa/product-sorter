# Performance Test Report
**Date:** 2026-07-01T16:17:43Z
**Workload:** mixed (70% list GET + 30% sort POST)

## Overall Results

| Session  | Total Req | Fail Rate | p50 (ms) | p95 (ms) | p99 (ms) | Max (ms) | Req/s | VUs |
|----------|-----------|-----------|----------|----------|----------|----------|-------|-----|
| 10k-products-quick | 18113 | 0% | 3 | 6 | 19 | 187 | 50.2 | 100 |
| 100k-products-quick | 9087 | 1% | 2 | 5 | 60001 | 60002 | 23.3 | 100 |
| 1M-products-quick | 1035 | 26% | 4 | 60001 | 60002 | 60005 | 2.2 | 100 |

## Per-Endpoint Latency

| Session  | Endpoint | p50 (ms) | p95 (ms) | p99 (ms) | Avg (ms) | Max (ms) |
|----------|----------|----------|----------|----------|----------|----------|
| 10k-products-quick | sort | 3 | 7 | 99 | 5.2 | 187 |
| 100k-products-quick | sort | 3 | 60000 | 60001 | 3379.1 | 60002 |
| 1M-products-quick | sort | 60001 | 60001 | 60002 | 52936.8 | 60005 |

## Checks

| Session  | Metric | Status | Detail |
|----------|--------|--------|--------|
| 10k-products-quick | check: list 200 | PASS | 12566/12566 |
| 10k-products-quick | check: sort 200 | PASS | 5447/5447 |
| 100k-products-quick | check: list 200 | PASS | 6349/6349 |
| 100k-products-quick | check: sort 200 | FAIL | 2492/2638 |
| 1M-products-quick | check: list 200 | FAIL | 627/680 |
| 1M-products-quick | check: sort 200 | FAIL | 33/255 |
