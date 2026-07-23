import { test, expect } from '@playwright/test';

test.describe('App shell smoke', () => {
  test('loads root route @smoke', async ({ page, baseURL }) => {
    test.skip(!baseURL, 'BASE_URL is not configured');

    await page.goto('/');
    await expect(page).toHaveURL(/.*/);
  });
});
