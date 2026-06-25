import { getToken } from './auth.js';

export function authHeaders() {
  return {
    'Authorization': `Bearer ${getToken()}`,
    'Content-Type': 'application/json',
    'X-Request-Id': `${__VU}-${__ITER}`,
  };
}
