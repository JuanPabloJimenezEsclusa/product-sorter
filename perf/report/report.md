# Performance Test Report
**Date:** 2026-07-07T14:56:15Z

## Overall Results

| Session  | Total Req | Fail Rate | p50 (ms) | p95 (ms) | p99 (ms) | Max (ms) | Req/s | VUs |
|----------|-----------|-----------|----------|----------|----------|----------|-------|-----|
| 10k-products | 36074 | 0% | 3 | 8 | 26 | 51 | 50 | 100 |
| 10k-products-quick | 18105 | 0% | 3 | 9 | 26 | 43 | 50.2 | 100 |
| 100k-products | 35992 | 0% | 3 | 7 | 90 | 1489 | 50 | 100 |
| 100k-products-quick | 18081 | 0% | 2 | 7 | 84 | 320 | 50.1 | 100 |
| 1M-products | 13485 | 33% | 5 | 5007 | 5010 | 5073 | 18.7 | 100 |
| 1M-products-quick | 8231 | 30% | 4 | 5007 | 5010 | 5073 | 18.2 | 100 |

## Per-Endpoint Latency

| Session  | Endpoint | p50 (ms) | p95 (ms) | p99 (ms) | Avg (ms) | Max (ms) |
|----------|----------|----------|----------|----------|----------|----------|
| 10k-products | sort | 3 | 18 | 28 | 4.7 | 51 |
| 10k-products-quick | sort | 4 | 21 | 28 | 5.2 | 43 |
| 100k-products | sort | 3 | 79 | 95 | 8.6 | 1382 |
| 100k-products-quick | sort | 3 | 73 | 87 | 7.9 | 320 |
| 1M-products | sort | 4971 | 5008 | 5011 | 2843.2 | 5073 |
| 1M-products-quick | sort | 4571 | 5008 | 5011 | 2559 | 5073 |

## Checks

| Session  | Metric | Status | Detail |
|----------|--------|--------|--------|
| 10k-products | check: list 200 | PASS | 14339/14339 |
| 10k-products | check: sort 200 | PASS | 21635/21635 |
| 10k-products-quick | check: list 200 | PASS | 7251/7251 |
| 10k-products-quick | check: sort 200 | PASS | 10754/10754 |
| 100k-products | check: list 200 | PASS | 14634/14634 |
| 100k-products | check: sort 200 | PASS | 21258/21258 |
| 100k-products-quick | check: list 200 | PASS | 7163/7163 |
| 100k-products-quick | check: sort 200 | PASS | 10818/10818 |
| 1M-products | check: list 200 | PASS | 5323/5323 |
| 1M-products | check: sort 200 | FAIL | 3531/8062 |
| 1M-products-quick | check: list 200 | PASS | 3236/3236 |
| 1M-products-quick | check: sort 200 | FAIL | 2421/4895 |
