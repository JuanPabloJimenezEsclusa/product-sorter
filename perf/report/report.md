# Performance Test Report
**Date:** 2026-07-10T13:42:16Z

## Overall Results

| Session  | Total Req | Fail Rate | p50 (ms) | p95 (ms) | p99 (ms) | Max (ms) | Req/s | VUs |
|----------|-----------|-----------|----------|----------|----------|----------|-------|-----|
| 10k-products | 36067 | 0% | 3 | 6 | 25 | 44 | 50.1 | 100 |
| 10k-products-quick | 18083 | 0% | 4 | 11 | 35 | 90 | 50.1 | 100 |
| 100k-products | 35999 | 0% | 3 | 7 | 85 | 713 | 49.9 | 100 |
| 100k-products-quick | 18024 | 0% | 3 | 8 | 152 | 312 | 50 | 100 |
| 1M-products | 18705 | 53% | 3 | 5007 | 5010 | 5171 | 25.9 | 100 |
| 1M-products-quick | 7958 | 37% | 5 | 5009 | 5013 | 5119 | 17.6 | 100 |

## Per-Endpoint Latency

| Session  | Endpoint | p50 (ms) | p95 (ms) | p99 (ms) | Avg (ms) | Max (ms) |
|----------|----------|----------|----------|----------|----------|----------|
| 10k-products | sort | 4 | 19 | 26 | 4.8 | 44 |
| 10k-products-quick | sort | 5 | 29 | 37 | 6.6 | 90 |
| 100k-products | sort | 4 | 76 | 88 | 8.5 | 123 |
| 100k-products-quick | sort | 4 | 134 | 160 | 12.7 | 312 |
| 1M-products | sort | 4 | 5007 | 5011 | 1576.2 | 5171 |
| 1M-products-quick | sort | 4750 | 5010 | 5014 | 2685.3 | 5119 |

## Checks

| Session  | Metric | Status | Detail |
|----------|--------|--------|--------|
| 10k-products | check: list 200 | PASS | 14448/14448 |
| 10k-products | check: sort 200 | PASS | 21519/21519 |
| 10k-products-quick | check: list 200 | PASS | 7236/7236 |
| 10k-products-quick | check: sort 200 | PASS | 10747/10747 |
| 100k-products | check: list 200 | PASS | 14365/14365 |
| 100k-products | check: sort 200 | PASS | 21534/21534 |
| 100k-products-quick | check: list 200 | PASS | 7246/7246 |
| 100k-products-quick | check: sort 200 | PASS | 10678/10678 |
| 1M-products | check: list 200 | FAIL | 4832/7470 |
| 1M-products | check: sort 200 | FAIL | 3818/11135 |
| 1M-products-quick | check: list 200 | FAIL | 2909/3100 |
| 1M-products-quick | check: sort 200 | FAIL | 1965/4758 |
