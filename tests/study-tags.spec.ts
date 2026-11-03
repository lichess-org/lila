import { test, expect } from '@playwright/test';

import { loginAs } from './helpers';

for (const [firstTag, firstValue, nextTag, nextValue] of [
  ['Date', '2022.01.01', 'Result', '1-0'],
  ['WhiteElo', '1500', 'WhiteTitle', 'GM'],
]) {
  test(`updates validation when adding ${nextTag} after ${firstTag}`, async ({ page }) => {
    await loginAs(page, 'teacher');
    await expect(page).toHaveURL('/');
    await page.goto('/study');
    await page.locator('form.new-study button').click();

    const studyUrl = page.url().split('/').slice(0, 5).join('/');
    try {
      await page.locator('dialog[open] .close-button').click();
      const table = page.locator('table.study__tags');
      const select = table.locator('select');
      const input = table.locator('tr').last().locator('input');
      const tagValue = (name: string) =>
        table
          .locator('tr')
          .filter({ has: page.locator('th').filter({ hasText: new RegExp(`^${name}$`) }) })
          .locator('input');

      await select.selectOption(firstTag);
      await input.fill(firstValue);
      await input.press('Enter');
      await expect(tagValue(firstTag)).toHaveValue(firstValue);

      // Reselecting the tag would fire change and hide the regression.
      await expect(select).toHaveValue(nextTag);
      if (nextTag === 'WhiteTitle') {
        await input.fill('1234');
        await input.press('Enter');
        await expect(input).toHaveValue('1234');
        await expect(tagValue(nextTag)).toHaveCount(0);
      }
      await input.fill(nextValue);
      await input.press('Enter');
      await expect(tagValue(nextTag)).toHaveValue(nextValue);

      await page.reload();
      await expect(tagValue(firstTag)).toHaveValue(firstValue);
      await expect(tagValue(nextTag)).toHaveValue(nextValue);
    } finally {
      const response = await page.request.post(`${studyUrl}/delete`, {
        headers: { Origin: new URL(studyUrl).origin },
      });
      expect(response.ok()).toBeTruthy();
    }
  });
}
