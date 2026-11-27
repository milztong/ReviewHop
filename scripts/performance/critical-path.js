import http from 'k6/http';
import { check, fail } from 'k6';
import execution from 'k6/execution';
import { Rate, Trend } from 'k6/metrics';

const apiBaseUrl = __ENV.API_BASE_URL || 'http://backend:8080';
const oidcBaseUrl = __ENV.OIDC_BASE_URL || 'http://keycloak:8080/realms/ankileet';
const clientId = __ENV.OIDC_CLIENT_ID || 'ankileet-web';
const clientSecret = __ENV.OIDC_CLIENT_SECRET;

const businessErrors = new Rate('business_errors');
const reviewCycleDuration = new Trend('review_cycle_duration', true);

export const options = {
  scenarios: {
    due_list: {
      executor: 'constant-arrival-rate',
      exec: 'listDueReviews',
      rate: 5,
      timeUnit: '1s',
      duration: '15s',
      preAllocatedVUs: 5,
      maxVUs: 20,
    },
    review_cycle: {
      executor: 'constant-arrival-rate',
      exec: 'completeReviewCycle',
      rate: 1,
      timeUnit: '1s',
      duration: '15s',
      preAllocatedVUs: 2,
      maxVUs: 10,
    },
  },
  thresholds: {
    business_errors: ['rate<0.01'],
    checks: ['rate>0.99'],
    dropped_iterations: ['count==0'],
    'http_req_duration{operation:due_list}': ['p(95)<500'],
    review_cycle_duration: ['p(95)<1500'],
  },
};

export function setup() {
  if (!clientSecret) {
    fail('OIDC_CLIENT_SECRET is required');
  }
  const tokenResponse = http.post(
    `${oidcBaseUrl}/protocol/openid-connect/token`,
    {
      grant_type: 'client_credentials',
      client_id: clientId,
      client_secret: clientSecret,
    },
    { tags: { operation: 'oidc_token' } },
  );
  const tokenAccepted = check(tokenResponse, {
    'OIDC token issued': (response) => response.status === 200,
  });
  if (!tokenAccepted) {
    fail(`OIDC token request failed with status ${tokenResponse.status}`);
  }

  const token = tokenResponse.json('access_token');
  const seedResponse = createLearningItem(token, uuid(1, 1), 'Capacity seed');
  if (seedResponse.status !== 201) {
    fail(`Capacity seed failed with status ${seedResponse.status}`);
  }
  return { token };
}

export function listDueReviews(data) {
  const response = http.get(`${apiBaseUrl}/api/v1/reviews/due`, {
    headers: authorization(data.token),
    tags: { operation: 'due_list' },
  });
  const accepted = check(response, {
    'due list returns 200': (result) => result.status === 200,
    'due list remains below rate limit': (result) => result.status !== 429,
  });
  businessErrors.add(!accepted);
}

export function completeReviewCycle(data) {
  const sequence = execution.scenario.iterationInTest + 1;
  const startedAt = Date.now();
  const creationKey = uuid(2, sequence);
  const createResponse = createLearningItem(
    data.token,
    creationKey,
    `Capacity item ${sequence}`,
  );
  const created = check(createResponse, {
    'manual item returns 201': (response) => response.status === 201,
    'write remains below rate limit': (response) => response.status !== 429,
  });
  if (!created) {
    businessErrors.add(true);
    return;
  }

  const reviewCardId = createResponse.json('reviewCardId');
  const completionResponse = http.post(
    `${apiBaseUrl}/api/v1/reviews/${reviewCardId}/completions`,
    JSON.stringify({
      rating: 'GOOD',
      startedAt: new Date(Date.now() - 1000).toISOString(),
      note: 'Capacity verification',
    }),
    {
      headers: {
        ...authorization(data.token),
        'Content-Type': 'application/json',
        'Idempotency-Key': uuid(3, sequence),
      },
      tags: { operation: 'review_completion' },
    },
  );
  const completed = check(completionResponse, {
    'review completion returns 200': (response) => response.status === 200,
    'completion remains below rate limit': (response) => response.status !== 429,
  });
  businessErrors.add(!completed);
  reviewCycleDuration.add(Date.now() - startedAt);
}

function createLearningItem(token, idempotencyKey, title) {
  return http.post(
    `${apiBaseUrl}/api/v1/learning-items/manual`,
    JSON.stringify({
      title,
      sourceUrl: `https://example.com/capacity/${idempotencyKey}`,
      tags: ['capacity-test'],
      firstCompletedAt: new Date(Date.now() - 60000).toISOString(),
    }),
    {
      headers: {
        ...authorization(token),
        'Content-Type': 'application/json',
        'Idempotency-Key': idempotencyKey,
      },
      tags: { operation: 'manual_item' },
    },
  );
}

function authorization(token) {
  return { Authorization: `Bearer ${token}` };
}

function uuid(namespace, sequence) {
  return `00000000-0000-4${namespace.toString().padStart(3, '0')}-8000-${sequence
    .toString()
    .padStart(12, '0')}`;
}
