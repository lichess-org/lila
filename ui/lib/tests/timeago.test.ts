import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { freshImport } from '../../.test/helpers.mts';

describe('test formatter', () => {
  test('lang code formatting', async () => {
    const cases: Array<[string, string]> = [
      ['en-US', 'Jan 1, 2024, 5:00 PM'],
      ['en-UK', '1 Jan 2024, 17:00'],
      ['fr', '1 janv. 2024, 17:00'],
    ];
    for (const [lang, expected] of cases) {
      document.documentElement.lang = lang;
      const { displayLocale, commonDateFormat } = await freshImport('lib/src/i18n');
      assert.equal(displayLocale, lang);
      assert.equal(commonDateFormat(new Date(2024, 0, 1, 17, 0, 0)), expected);
    }
  });

  test('arabic lang code uses the gregorian calendar', async () => {
    const langs = ['ar-ae', 'ar-ly', 'ar-ye'];
    for (const lang of langs) {
      document.documentElement.lang = lang;
      const { displayLocale, commonDateFormat } = await freshImport('lib/src/i18n');
      assert.equal(displayLocale, 'ar-ly');
      assert.equal(commonDateFormat(new Date(2024, 0, 1, 17, 0, 0)), '1 يناير 2024، 5:00 م');
    }
  });
});
