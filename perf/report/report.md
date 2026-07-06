# Performance Test Report
**Date:** 2026-07-06T02:42:02Z
**Workload:** mixed (40% list GET + 60% sort POST)

## Overall Results

| Session  | Total Req | Fail Rate | p50 (ms) | p95 (ms) | p99 (ms) | Max (ms) | Req/s | VUs |
|----------|-----------|-----------|----------|----------|----------|----------|-------|-----|
| 10k-products-quick | 18137 | 0% | 2 | 5 | 17 | 35 | 50.3 | 100 |
| 100k-products-quick | 18107 | 0% | 1 | 3 | 77 | 120 | 50.2 | 100 |
| 1M-products-quick | 8341 | 29% | 2 | 5004 | 5007 | 5048 | 18.5 | 100 |

## Per-Endpoint Latency

| Session  | Endpoint | p50 (ms) | p95 (ms) | p99 (ms) | Avg (ms) | Max (ms) |
|----------|----------|----------|----------|----------|----------|----------|
| 10k-products-quick | sort | 2 | 13 | 18 | 3 | 35 |
| 100k-products-quick | sort | 2 | 71 | 79 | 6.2 | 120 |
| 1M-products-quick | sort | 871 | 5005 | 5007 | 2457.3 | 5048 |

## Checks

| Session  | Metric | Status | Detail |
|----------|--------|--------|--------|
| 10k-products-quick | check: list 200 | PASS | 7412/7412 |
| 10k-products-quick | check: sort 200 | PASS | 10625/10625 |
| 100k-products-quick | check: list 200 | PASS | 7304/7304 |
| 100k-products-quick | check: sort 200 | PASS | 10703/10703 |
| 1M-products-quick | check: list 200 | PASS | 3183/3183 |
| 1M-products-quick | check: sort 200 | FAIL | 2618/5058 |
