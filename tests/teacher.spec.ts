import { test, expect } from '@playwright/test';

import { loginAs, messageText } from './helpers';

test.beforeEach(async ({ page }) => {
  await loginAs(page, 'teacher');
});

test('can message their student', async ({ page }) => {
  const msg = messageText();

  await page.goto('/inbox/student1');
  await page.getByTestId('msg-textarea').fill(msg);
  await page.getByTestId('msg-send-button').click();

  await expect(page.getByTestId('msg-textarea')).toBeEmpty();
  await expect(page.locator('group').getByText(msg)).toBeVisible();
});

test('cannot message an account who is not their student', async ({ page }) => {
  await page.goto('/inbox/kid');
  await expect(page.getByText("Kid doesn't accept new messages")).toBeVisible();
});
