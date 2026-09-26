import { test, expect } from '@playwright/test';

import { loginAs } from './helpers';

test.beforeEach(async ({ page }) => {
  await loginAs(page, 'student1');
});

test('homepage is in kid mode', async ({ page }) => {
  await expect(page.locator('body')).toHaveClass(/kid/);
  await expect(page.getByTestId('site-title')).toContainText(':)');
});

test('pages are blocked', async ({ page }) => {
  const blockedPaths = ['/forum', '/streamer'];

  for (const path of blockedPaths) {
    const response = await page.goto(path);
    expect(response?.status()).toBe(404);
  }
});

test('only Lichess blog', async ({ page }) => {
  await page.goto('/blog');
  await expect(page).toHaveURL('/@/Lichess/blog');
});
