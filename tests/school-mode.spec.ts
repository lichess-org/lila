import { test, expect } from '@playwright/test';

test('normal request is not in kid mode', async ({ page }) => {
  await page.goto('/');
  await expect(page.locator('body')).not.toHaveClass(/kid/);
});

// test('request with X-School-Mode header is in kid mode', async ({ browser }) => {
//   const context = await browser.newContext({
//     extraHTTPHeaders: { 'X-School-Mode': 'true' },
//   });
//   const page = await context.newPage();
//   await page.goto('/');
//   await expect(page.locator('body')).toHaveClass(/kid/);
//   await context.close();
// });
