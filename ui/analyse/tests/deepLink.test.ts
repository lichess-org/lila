import { expect, it } from 'vitest';
import { broadcasterDeepLink } from '../src/study/relay/deepLink';

it('creates deep link from URL', () => {
  expect(
    broadcasterDeepLink('https://lichess.org/broadcast/fide-grand-swiss-2025--open/round-1/xSCoiNg0'),
  ).toBe('lichess-broadcaster://broadcast/fide-grand-swiss-2025--open/round-1/xSCoiNg0');
});

it('throws on invalid URL', () => {
  expect(() => broadcasterDeepLink('invalid-url')).toThrow('Invalid URL: invalid-url');
});
