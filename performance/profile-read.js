import http from 'k6/http';
import { check, fail, sleep } from 'k6';

const baseUrl = (__ENV.BASE_URL || 'https://localhost').replace(/\/+$/, '');

export const options = {
  insecureSkipTLSVerify: __ENV.INSECURE_SKIP_TLS_VERIFY === 'true',
  scenarios: {
    profile_reads: {
      executor: 'constant-vus',
      vus: Number(__ENV.PERF_VUS || 10),
      duration: __ENV.PERF_DURATION || '2m',
      gracefulStop: '15s',
    },
  },
  thresholds: {
    'checks{endpoint:profile-read}': ['rate>0.99'],
    'http_req_failed{endpoint:profile-read}': ['rate<0.01'],
    'http_req_duration{endpoint:profile-read}': ['p(95)<500', 'p(99)<1000'],
  },
};

function jsonHeaders(token) {
  return {
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  };
}

export function setup() {
  const username = __ENV.K6_USERNAME;
  const password = __ENV.K6_PASSWORD;
  if (!username || !password) {
    fail('K6_USERNAME and K6_PASSWORD must identify a dedicated performance-test user');
  }

  const loginResponse = http.post(
    `${baseUrl}/api/v1/auth/login`,
    JSON.stringify({ username, password }),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { endpoint: 'setup-login' },
    },
  );

  const loginSucceeded = check(loginResponse, {
    'performance user can log in': (response) => response.status === 200,
  });
  if (!loginSucceeded) {
    fail(`Login failed with HTTP ${loginResponse.status}`);
  }

  const token = loginResponse.json('token');
  if (!token) {
    fail('Login response did not contain a JWT');
  }

  const profileEmail = __ENV.K6_PROFILE_EMAIL || `${username}@performance.invalid`;
  const profileResponse = http.put(
    `${baseUrl}/api/v1/users/me`,
    JSON.stringify({ fullName: 'Performance Test User', email: profileEmail }),
    {
      ...jsonHeaders(token),
      tags: { endpoint: 'setup-profile' },
    },
  );

  if (profileResponse.status !== 200) {
    fail(`Profile setup failed with HTTP ${profileResponse.status}`);
  }

  return { token };
}

export default function (data) {
  const response = http.get(
    `${baseUrl}/api/v1/users/me`,
    {
      ...jsonHeaders(data.token),
      tags: { endpoint: 'profile-read' },
    },
  );

  check(
    response,
    {
      'profile read returns 200': (result) => result.status === 200,
      'profile response has username': (result) => Boolean(result.json('data.username')),
    },
    { endpoint: 'profile-read' },
  );

  sleep(1);
}
