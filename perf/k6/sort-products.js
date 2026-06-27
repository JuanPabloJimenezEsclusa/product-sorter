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
    'http_req_failed{endpoint:sort}': [{ threshold: 'rate<0.05', abortOnFail: false }],
  },
};

function randomWeights() {
  const sales = (Math.random() * 0.6 + 0.2).toFixed(2);
  const stock = (1 - sales).toFixed(2);
  return JSON.stringify({ weights: { salesUnits: parseFloat(sales), stockRatio: parseFloat(stock) } });
}

export default function () {
  const headers = authHeaders();
  const body = randomWeights();

  const res = http.post(`${BASE}/api/v1/products/sort?size=20`, body, {
    headers,
    tags: { endpoint: 'sort' },
  });

  check(res, {
    'status 200': (r) => r.status === 200,
  });

  sleep(1);
}
