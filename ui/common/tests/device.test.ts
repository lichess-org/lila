import { describe, expect, test } from 'vitest';
import { isVersionCompatible } from '../src/device';

describe('test isVersionCompatible', () => {
  test('isVersionCompatible', () => {
    expect(isVersionCompatible('15b1', { atLeast: '1.1.1', below: '15.0.1' })).toBe(true);
    expect(isVersionCompatible('10.2', { atLeast: '10.10' })).toBe(false);
    expect(isVersionCompatible('15.4.2', { atLeast: '15', below: '15.4.3' })).toBe(true);
    expect(isVersionCompatible('15.4.2', { atLeast: '14.99.99', below: '15.4.2' })).toBe(false);
    expect(isVersionCompatible('15', { below: '15' })).toBe(false);
    expect(isVersionCompatible('15', { below: '15.0' })).toBe(false);
    expect(isVersionCompatible('15', { below: '15.1' })).toBe(true);
    expect(isVersionCompatible('15.1.0', { below: '15.1' })).toBe(false);
    expect(isVersionCompatible('15.1', { atLeast: '15.1', below: '15.1' })).toBe(false);
    expect(isVersionCompatible('122_2a7', { below: '122.2' })).toBe(false);
    expect(isVersionCompatible('122_2a7', { atLeast: '122.2' })).toBe(true);
  });
});
