# Performance Test Report
**Date:** 2026-07-04T16:28:06Z
**Workload:** mixed (40% list GET + 60% sort POST)

## Overall Results

| Session  | Total Req | Fail Rate | p50 (ms) | p95 (ms) | p99 (ms) | Max (ms) | Req/s | VUs |
|----------|-----------|-----------|----------|----------|----------|----------|-------|-----|
| 10k-products-quick | 18104 | 0% | 3 | 7 | 16 | 73 | 50.2 | 100 |
| 100k-products-quick | 18092 | 0% | 3 | 5 | 12 | 563 | 50.2 | 100 |
| 1M-products-quick | 16802 | 4% | 3 | 15 | 5007 | 5038 | 37.2 | 100 |

## Per-Endpoint Latency

| Session  | Endpoint | p50 (ms) | p95 (ms) | p99 (ms) | Avg (ms) | Max (ms) |
|----------|----------|----------|----------|----------|----------|----------|
| 10k-products-quick | sort | 4 | 8 | 23 | 4.3 | 73 |
| 100k-products-quick | sort | 3 | 5 | 8 | 5.8 | 563 |
| 1M-products-quick | sort | 3 | 5004 | 5008 | 392.5 | 5038 |

## Checks

| Session  | Metric | Status | Detail |
|----------|--------|--------|--------|
| 10k-products-quick | check: list 200 | PASS | 7190/7190 |
| 10k-products-quick | check: sort 200 | PASS | 10814/10814 |
| 100k-products-quick | check: list 200 | PASS | 7154/7154 |
| 100k-products-quick | check: sort 200 | PASS | 10838/10838 |
| 1M-products-quick | check: list 200 | PASS | 6602/6602 |
| 1M-products-quick | check: sort 200 | FAIL | 9372/10100 |
