import { describe, expect, test, vi } from 'vitest';
import { once } from '../src/storage';

describe('test once', () => {
  test('once', async () => {
    expect(once('foo')).toBe(true);

    // subsequent calls should return false
    expect(once('foo')).toBe(false);
    expect(once('foo')).toBe(false);
    vi.useFakeTimers();
    expect(once('secs', { seconds: 1 })).toBe(true);
    expect(once('secs', { seconds: 1 })).toBe(false);
    vi.advanceTimersByTime(1050);
    expect(once('secs', { seconds: 1 })).toBe(true);
    vi.useRealTimers();
  });
});
