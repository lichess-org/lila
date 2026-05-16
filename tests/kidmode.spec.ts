import { test, expect } from '@playwright/test';

test.beforeEach(async ({ page }) => {
  await page.goto('/');
  await page.getByTestId('login').click();
  await page.getByTestId('username').fill('student1');
  await page.getByTestId('password').fill('password');
  await page.getByTestId('login-submit').click();
  await page.waitForLoadState('networkidle'); 
});

test('login to kidmode', async ({ page }) => {
  await expect(page.locator('body')).toHaveClass(/kid/);
  await expect(page.getByTestId('site-title')).toContainText(':)');
});

test('cannot message another student', async ({ page }) => {
  await page.goto('/inbox/Student2');
  await expect(page.getByText('Student2 doesn\'t accept new messages')).toBeVisible();
});
