import { test, expect } from '@playwright/test';

test.describe('Phase 1 Foundation and Defence @phase1', () => {
  test('UI can trigger hardening flow through API boundary', async ({ page }) => {
    const responsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/v1/hardening') &&
      response.request().method() === 'POST'
    );

    await page.goto('/');
    await page.getByRole('button', { name: /run hardening/i }).click();

    const response = await responsePromise;
    expect(response.status()).toBe(202);

    const body = await response.json();
    expect(body.status).toBe('accepted');
    expect(body.message).toBe('Hardening request accepted');

    await expect(page.getByRole('status')).toContainText('Hardening request accepted');
  });

  test('UI shows policy violation message from API boundary', async ({ page }) => {
    await page.route('**/api/v1/hardening', async (route) => {
      await route.fulfill({
        status: 422,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'POLICY_VIOLATION',
          message: 'profile is not supported.'
        })
      });
    });

    await page.goto('/');
    await page.getByRole('button', { name: /run hardening/i }).click();

    await expect(page.getByRole('status')).toContainText('profile is not supported.');
  });

  test('UI shows unauthorized operator message from API boundary', async ({ page }) => {
    await page.route('**/api/v1/hardening', async (route) => {
      await route.fulfill({
        status: 422,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'POLICY_VIOLATION',
          message: 'requestedBy is not authorized.'
        })
      });
    });

    await page.goto('/');
    await page.getByRole('button', { name: /run hardening/i }).click();

    await expect(page.getByRole('status')).toContainText('requestedBy is not authorized.');
  });

  test('UI shows unauthorized tenant message from API boundary', async ({ page }) => {
    await page.route('**/api/v1/hardening', async (route) => {
      await route.fulfill({
        status: 422,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'POLICY_VIOLATION',
          message: 'tenantId is not authorized.'
        })
      });
    });

    await page.goto('/');
    await page.getByRole('button', { name: /run hardening/i }).click();

    await expect(page.getByRole('status')).toContainText('tenantId is not authorized.');
  });
});
