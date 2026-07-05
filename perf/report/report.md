# Performance Test Report
**Date:** 2026-07-04T20:55:39Z
**Workload:** mixed (40% list GET + 60% sort POST)

## Overall Results

| Session  | Total Req | Fail Rate | p50 (ms) | p95 (ms) | p99 (ms) | Max (ms) | Req/s | VUs |
|----------|-----------|-----------|----------|----------|----------|----------|-------|-----|
| 10k-products-quick | 18097 | 0% | 3 | 9 | 37 | 54 | 50.2 | 100 |
| 100k-products-quick | 17860 | 0% | 3 | 7 | 322 | 2690 | 49.5 | 100 |
| 1M-products-quick | 5832 | 50% | 4597 | 5009 | 5013 | 5031 | 12.9 | 100 |

## Per-Endpoint Latency

| Session  | Endpoint | p50 (ms) | p95 (ms) | p99 (ms) | Avg (ms) | Max (ms) |
|----------|----------|----------|----------|----------|----------|----------|
| 10k-products-quick | sort | 4 | 28 | 38 | 5.8 | 54 |
| 100k-products-quick | sort | 4 | 297 | 349 | 28.3 | 2690 |
| 1M-products-quick | sort | 5006 | 5011 | 5015 | 4385.8 | 5031 |

## Checks

| Session  | Metric | Status | Detail |
|----------|--------|--------|--------|
| 10k-products-quick | check: list 200 | PASS | 7395/7395 |
| 10k-products-quick | check: sort 200 | PASS | 10602/10602 |
| 100k-products-quick | check: list 200 | PASS | 7216/7216 |
| 100k-products-quick | check: sort 200 | PASS | 10544/10544 |
| 1M-products-quick | check: list 200 | PASS | 2329/2329 |
| 1M-products-quick | check: sort 200 | FAIL | 472/3403 |
