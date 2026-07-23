import { test, expect } from '@playwright/test';

test.describe('Phase 1 Foundation and Defence @phase1', () => {
  test('UI can trigger hardening flow through API boundary', async ({ page }) => {
    test.fixme(true, 'Enable when Angular hardening page and API route are available');

    const requests: string[] = [];
    await page.route('**/api/**', async (route) => {
      requests.push(route.request().url());
      await route.continue();
    });

    await page.goto('/hardening');
    await page.getByRole('button', { name: /run hardening/i }).click();

    await expect.poll(() => requests.some((u) => /\/api\/(v1\/)?hardening/.test(u))).toBeTruthy();
  });
});
