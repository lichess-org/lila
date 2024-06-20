import { beforeEach, describe, expect, test, vi } from 'vitest';

describe('test formatter', () => {
  beforeEach(() => {
    vi.resetModules();
  });

  test.each([
    ['en-US', 'Jan 1, 2024, 5:00 PM'],
    ['en-UK', '1 Jan 2024, 17:00'],
    ['fr', '1 janv. 2024, 17:00'],
  ])('lang code formatting', async (lang, expected) => {
    document.documentElement.lang = lang;
    const formatter = await import('../src/timeago').then(m => m.formatter);
    expect(formatter()(new Date(2024, 0, 1, 17, 0, 0))).toBe(expected);
  });

  test.each([
    ['ar-ae'],
    ['ar-bh'],
    ['ar-dz'],
    ['ar-eg'],
    ['ar-iq'],
    ['ar-jo'],
    ['ar-kw'],
    ['ar-lb'],
    ['ar-ly'],
    ['ar-ma'],
    ['ar-om'],
    ['ar-qa'],
    ['ar-sa'],
    ['ar-sy'],
    ['ar-tn'],
    ['ar-ye'],
  ])('arabic lang code uses the gregorian calendar', async lang => {
    document.documentElement.lang = lang;
    const formatter = await import('../src/timeago').then(m => m.formatter);
    expect(formatter()(new Date(2024, 0, 1, 17, 0, 0))).toBe('1 يناير 2024، 5:00 م');
  });
});
