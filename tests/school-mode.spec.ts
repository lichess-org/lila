import { test, expect } from '@playwright/test';

test('normal request is not in kid mode', async ({ page }) => {
  await page.goto('/');
  await expect(page.locator('body')).not.toHaveClass(/kid/);
});
