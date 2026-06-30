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
    'http_req_duration{endpoint:list}': [{ threshold: 'p(95)<2000', abortOnFail: false }],
    'http_req_failed{endpoint:list}': [{ threshold: 'rate<0.01', abortOnFail: false }],
  },
};

export default function listProducts() {
  const headers = authHeaders();

  const res = http.get(`${BASE}/api/v1/products?size=20`, {
    headers,
    tags: { endpoint: 'list' },
  });

  check(res, {
    'status 200': (r) => r.status === 200,
    'has data': (r) => {
      try {
        const body = r.json();
        return body.data && Array.isArray(body.data);
      } catch {
        return false;
      }
    },
  });

  sleep(1);
}
