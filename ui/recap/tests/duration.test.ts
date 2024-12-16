import { expect, test } from 'vitest';
import { formatDuration } from '../src/util';

test('singular and plural time units', () => {
  expect(formatDuration(0)).toBe('0 hours<br>0 minutes');
  expect(formatDuration(1)).toBe('0 hours<br>0 minutes');
  expect(formatDuration(2)).toBe('0 hours<br>0 minutes');

  expect(formatDuration(60)).toBe('0 hours<br>1 minute');
  expect(formatDuration(120)).toBe('0 hours<br>2 minutes');

  expect(formatDuration(60 * 60 * 1)).toBe('1 hour<br>0 minutes');
  expect(formatDuration(60 * 60 * 2)).toBe('2 hours<br>0 minutes');

  expect(formatDuration(60 * 60 * 1 + 60 * 1)).toBe('1 hour<br>1 minute');
  expect(formatDuration(60 * 60 * 1 + 60 * 2)).toBe('1 hour<br>2 minutes');
  expect(formatDuration(60 * 60 * 2 + 60 * 1)).toBe('2 hours<br>1 minute');
  expect(formatDuration(60 * 60 * 2 + 60 * 2)).toBe('2 hours<br>2 minutes');

  expect(formatDuration(60 * 60 * 24 * 1)).toBe('1 day<br>0 hours');
  expect(formatDuration(60 * 60 * 24 * 2)).toBe('2 days<br>0 hours');

  expect(formatDuration(60 * 60 * 24 * 1 + 60 * 60 * 1 + 60 * 1)).toBe('1 day<br>1 hour');
  expect(formatDuration(60 * 60 * 24 * 2 + 60 * 60 * 2 + 60 * 2)).toBe('2 days<br>2 hours');
});
