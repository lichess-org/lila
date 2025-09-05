import { expect, it } from 'vitest';
import { openInApp } from '../src/study/relay/deepLink';

it('creates deep link from URL', () => {
  expect(openInApp('https://lichess.org/broadcast/fide-grand-swiss-2025--open/round-1/xSCoiNg0')).toBe(
    'lichess-broadcaster://broadcast/fide-grand-swiss-2025--open/round-1/xSCoiNg0',
  );
  expect(() => openInApp('invalid-url')).toThrow('Invalid URL: invalid-url');
});
