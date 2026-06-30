import http from 'k6/http';
import { check, sleep } from 'k6';
import { authHeaders } from './lib/headers.js';

const BASE = __ENV.APP_URL || 'http://localhost:8880';

export const options = {
  stages: [
    { duration: '30s', target: 5 },
    { duration: '3m',  target: 10 },
    { duration: '1m',  target: 50 },
    { duration: '3m',  target: 50 },
    { duration: '1m',  target: 100 },
    { duration: '3m',  target: 100 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    'http_req_duration{endpoint:sort}': [{ threshold: 'p(95)<5000', abortOnFail: false }],
    'http_req_failed': [{ threshold: 'rate<0.02', abortOnFail: false }],
  },
};

function randomWeights() {
  const sales = (Math.random() * 0.6 + 0.2).toFixed(2);
  return JSON.stringify({
    weights: { salesUnits: Number.parseFloat(sales), stockRatio: Number.parseFloat((1 - sales).toFixed(2)) },
  });
}

export default function executeWorkload() {
  const headers = authHeaders();

  if (Math.random() < 0.7) {
    const res = http.get(`${BASE}/api/v1/products?size=20`, {
      headers,
      tags: { endpoint: 'list' },
    });
    check(res, { 'list 200': (r) => r.status === 200 });
  } else {
    const res = http.post(`${BASE}/api/v1/products/sort?size=20`, randomWeights(), {
      headers,
      tags: { endpoint: 'sort' },
    });
    check(res, { 'sort 200': (r) => r.status === 200 });
  }

  sleep(1);
}
