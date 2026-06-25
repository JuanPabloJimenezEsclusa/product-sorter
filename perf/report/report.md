# Performance Test Report
**Date:** 2026-07-07T19:28:28Z

## Overall Results

| Session  | Total Req | Fail Rate | p50 (ms) | p95 (ms) | p99 (ms) | Max (ms) | Req/s | VUs |
|----------|-----------|-----------|----------|----------|----------|----------|-------|-----|
| 10k-products | 36074 | 0% | 3 | 8 | 26 | 51 | 50 | 100 |
| 10k-products-quick | 18106 | 0% | 3 | 9 | 26 | 43 | 50.2 | 100 |
| 100k-products | 35992 | 0% | 3 | 7 | 90 | 1489 | 50 | 100 |
| 100k-products-quick | 18085 | 0% | 3 | 7 | 81 | 103 | 50.1 | 100 |
| 1M-products | 13485 | 33% | 5 | 5007 | 5010 | 5073 | 18.7 | 100 |
| 1M-products-quick | 8355 | 29% | 4 | 5007 | 5009 | 5072 | 18.6 | 100 |

## Per-Endpoint Latency

| Session  | Endpoint | p50 (ms) | p95 (ms) | p99 (ms) | Avg (ms) | Max (ms) |
|----------|----------|----------|----------|----------|----------|----------|
| 10k-products | sort | 3 | 18 | 28 | 4.7 | 51 |
| 10k-products-quick | sort | 4 | 19 | 27 | 5.1 | 43 |
| 100k-products | sort | 3 | 79 | 95 | 8.6 | 1382 |
| 100k-products-quick | sort | 3 | 71 | 83 | 7.7 | 103 |
| 1M-products | sort | 4971 | 5008 | 5011 | 2843.2 | 5073 |
| 1M-products-quick | sort | 4500 | 5007 | 5011 | 2540.6 | 5072 |

## Checks

| Session  | Metric | Status | Detail |
|----------|--------|--------|--------|
| 10k-products | check: list 200 | PASS | 14339/14339 |
| 10k-products | check: sort 200 | PASS | 21635/21635 |
| 10k-products-quick | check: list 200 | PASS | 7164/7164 |
| 10k-products-quick | check: sort 200 | PASS | 10842/10842 |
| 100k-products | check: list 200 | PASS | 14634/14634 |
| 100k-products | check: sort 200 | PASS | 21258/21258 |
| 100k-products-quick | check: list 200 | PASS | 7214/7214 |
| 100k-products-quick | check: sort 200 | PASS | 10771/10771 |
| 1M-products | check: list 200 | PASS | 5323/5323 |
| 1M-products | check: sort 200 | FAIL | 3531/8062 |
| 1M-products-quick | check: list 200 | PASS | 3381/3381 |
| 1M-products-quick | check: sort 200 | FAIL | 2427/4874 |
