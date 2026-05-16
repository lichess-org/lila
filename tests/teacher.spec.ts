import { test, expect } from '@playwright/test';
import { loginAs } from './helpers';

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
