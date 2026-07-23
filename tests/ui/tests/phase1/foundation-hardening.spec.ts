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
});
