import { test, expect } from '@playwright/test';

test.describe('Phase 2 Proxy Management @phase2', () => {
  test('shows no-server state and routes to installation procedure', async ({ page }) => {
    await page.goto('/');

    await page.getByRole('button', { name: /thor login/i }).click();
    await expect(page.getByRole('heading', { name: /gsserver ui/i })).toBeVisible();

    await page.getByRole('link', { name: /proxy/i }).click();
    await expect(page).toHaveURL(/\/proxy$/);

    await expect(page.getByRole('status')).toContainText(
      'No NGINX or Apache server is currently running on this system.'
    );
    await expect(page.getByRole('link', { name: /start proxy installation procedure/i })).toBeVisible();

    await page.getByRole('link', { name: /start proxy installation procedure/i }).click();
    await expect(page).toHaveURL(/\/proxy\/install$/);
    await expect(page.getByRole('heading', { name: /proxy installation procedure/i })).toBeVisible();
  });
});
