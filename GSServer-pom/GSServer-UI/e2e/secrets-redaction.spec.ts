import { test, expect } from '@playwright/test';

/**
 * E2E Test: Secrets Redaction (Phase 1)
 *
 * Stack-based TDD workflow:
 * 1. Write this E2E test (defines end-to-end contract)
 * 2. Run → fails at Handler boundary
 * 3. Pause E2E, write Handler focused IT
 * 4. Implement Handler → resume E2E
 * 5. Fails at Service boundary, repeat
 * 6. Continue until E2E passes
 */

test.describe('Secrets Redaction', () => {

  test('POST /api/v1/hardening with error → response secrets redacted', async ({ request }) => {
    // Arrange: trigger hardening with invalid input to cause error
    const response = await request.post('http://localhost:8080/api/v1/hardening', {
      data: {
        serverId: 'test-server-invalid'  // Will trigger error path
      }
    });

    // Act: inspect error response
    expect(response.status()).toBe(500);

    const body = await response.json();

    // Assert: structured error response (not raw exception)
    expect(body).toHaveProperty('errorId');
    expect(body).toHaveProperty('code');
    expect(body).toHaveProperty('message');
    expect(body).toHaveProperty('timestamp');
    expect(body).toHaveProperty('correlationId');
    expect(body).toHaveProperty('severity');

    // Assert: secrets redacted (no credentials, no paths, no sensitive data)
    const errorMessage = body.message;
    const errorDetails = JSON.stringify(body);

    // Should not contain common secrets patterns
    expect(errorMessage).not.toMatch(/password|passwd|pwd/i);
    expect(errorMessage).not.toMatch(/user:.*:.*@/);  // DB connection strings
    expect(errorMessage).not.toMatch(/^\/.+\/\.ssh/);  // SSH key paths
    expect(errorMessage).not.toMatch(/api[_-]?key|api[_-]?token/i);
    expect(errorMessage).not.toMatch(/bearer\s+[a-z0-9]+/i);  // Auth tokens

    // Error message should be human-readable
    expect(errorMessage.length).toBeGreaterThan(5);
    expect(errorMessage.length).toBeLessThan(500);

    // Correlation ID should be valid UUID format
    expect(body.correlationId).toMatch(/^[0-9a-f-]{36}$/i);

    // Timestamp should be ISO 8601
    expect(body.timestamp).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/);
  });

  test('POST /api/v1/hardening success → response has correlationId', async ({ request }) => {
    const response = await request.post('http://localhost:8080/api/v1/hardening', {
      data: {
        serverId: 'valid-server'
      }
    });

    expect(response.status()).toBe(200);

    const body = await response.json();

    // Success response should also have correlation ID for tracing
    expect(body).toHaveProperty('correlationId');
    expect(body.correlationId).toMatch(/^[0-9a-f-]{36}$/i);
  });

  test('Multiple requests have different correlationIds', async ({ request }) => {
    const response1 = await request.post('http://localhost:8080/api/v1/hardening', {
      data: { serverId: 'test-1' }
    });

    const response2 = await request.post('http://localhost:8080/api/v1/hardening', {
      data: { serverId: 'test-2' }
    });

    const body1 = await response1.json();
    const body2 = await response2.json();

    // Each request should have unique correlation ID
    expect(body1.correlationId).not.toEqual(body2.correlationId);
  });

});
