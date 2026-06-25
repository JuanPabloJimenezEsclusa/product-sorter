import http from 'k6/http';

const KEYCLOAK_URL = __ENV.KEYCLOAK_URL || 'http://keycloak:8080';
const REALM = 'product-sorter';
const CLIENT_ID = 'product-sorter-client';
const CLIENT_SECRET = __ENV.KEYCLOAK_CLIENT_SECRET || 'product-sorter-secret';

let cachedToken = null;
let tokenExpiry = 0;

export function getToken() {
  const now = Date.now();
  if (cachedToken && now < tokenExpiry) {
    return cachedToken;
  }

  const resp = http.post(
    `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`,
    {
      grant_type: 'client_credentials',
      client_id: CLIENT_ID,
      client_secret: CLIENT_SECRET,
    },
    { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }
  );

  if (resp.status !== 200) {
    throw new Error(`Token request failed: ${resp.status} ${resp.body}`);
  }

  const body = resp.json();
  cachedToken = body.access_token;
  tokenExpiry = now + (body.expires_in - 30) * 1000;
  return cachedToken;
}
