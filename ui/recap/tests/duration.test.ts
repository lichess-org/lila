import { expect, test } from 'vitest';
import { showDuration } from '../src/util';

test('singular and plural time units', () => {
  expect(showDuration(0)).toBe('0 minutes');
  expect(showDuration(1)).toBe('0 minutes');
  expect(showDuration(2)).toBe('0 minutes');

  expect(showDuration(60)).toBe('1 minute');
  expect(showDuration(120)).toBe('2 minutes');

  expect(showDuration(60 * 60 * 1)).toBe('1 hour');
  expect(showDuration(60 * 60 * 2)).toBe('2 hours');

  expect(showDuration(60 * 60 * 1 + 60 * 1)).toBe('1 hour and 1 minute');
  expect(showDuration(60 * 60 * 1 + 60 * 2)).toBe('1 hour and 2 minutes');
  expect(showDuration(60 * 60 * 2 + 60 * 1)).toBe('2 hours and 1 minute');
  expect(showDuration(60 * 60 * 2 + 60 * 2)).toBe('2 hours and 2 minutes');

  expect(showDuration(60 * 60 * 24 * 1)).toBe('1 day');
  expect(showDuration(60 * 60 * 24 * 2)).toBe('2 days');

  expect(showDuration(60 * 60 * 24 * 1 + 60 * 60 * 1 + 60 * 1)).toBe('1 day and 1 hour');
  expect(showDuration(60 * 60 * 24 * 2 + 60 * 60 * 2 + 60 * 2)).toBe('2 days and 2 hours');
});
