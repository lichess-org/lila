import { type Page } from '@playwright/test';

export async function loginAs(page: Page, username: string) {
  await page.goto('/login');
  await page.getByTestId('username').fill(username);
  await page.getByTestId('password').fill('password');
  await page.getByTestId('login-submit').click();
  await page.waitForLoadState('networkidle');
}

export function messageText() {
  return `hi, the time is ${new Date().toISOString()}`;
}
