import http from 'k6/http';
import { check } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8000';
const token = __ENV.AUTH_TOKEN;

export const options = {
  stages: [
    { duration: '15s', target: 50 },
    { duration: '2m', target: 50 },
    { duration: '15s', target: 0 },
  ],
  thresholds: {
    'http_req_duration{endpoint:search}': ['p(95)<300'],
    'http_req_failed': ['rate<0.05'],
  },
};

function headers() {
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export default function () {
  const search = http.get(`${baseUrl}/api/search?q=release`, {
    headers: headers(),
    tags: { endpoint: 'search' },
  });
  check(search, { 'search responds successfully': (response) => response.status === 200 });

  const versionId = __ENV.VERSION_ID;
  if (versionId) {
    const download = http.get(`${baseUrl}/api/catalog/download/${versionId}`, {
      headers: headers(),
      tags: { endpoint: 'download' },
    });
    check(download, { 'download endpoint responds': (response) => response.status < 500 });
  }
}
