import { test, expect, Page } from '@playwright/test';

async function loginAs(page: Page, username: string) {
  await page.goto('/login');
  await page.getByTestId('username').fill(username);
  await page.getByTestId('password').fill('password');
  await page.getByTestId('login-submit').click();
  await page.waitForLoadState('networkidle');
}

test.beforeEach(async ({ page }) => {
  await loginAs(page, 'teacher');
});

test('can message their student', async ({ page }) => {
  await page.goto('/inbox/student1');

  await page.getByTestId('msg-textarea').fill('hi');
  await page.getByTestId('msg-send-button').click();
});

test('cannot message a kid who is not their student', async ({ page }) => {
  await page.goto('/inbox/kid');
  await expect(page.getByText("Kid doesn't accept new messages")).toBeVisible();
});
