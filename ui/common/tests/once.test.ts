import { describe, expect, test } from 'vitest';
import { once } from '../src/storage';

describe('test once', () => {
  test('once', () => {
    expect(once('foo')).toBe(true);

    // subsequent calls should return false
    expect(once('foo')).toBe(false);
    expect(once('foo')).toBe(false);

    expect(once('foo', 'always')).toBe(true);
  });
});
