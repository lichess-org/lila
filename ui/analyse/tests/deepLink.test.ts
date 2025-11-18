import { test } from 'node:test';
import assert from 'node:assert/strict';
import { broadcasterDeepLink } from '../src/study/relay/deepLink';

test('creates deep link from URL', () => {
  assert.equal(
    broadcasterDeepLink('https://lichess.org/broadcast/fide-grand-swiss-2025--open/round-1/xSCoiNg0'),
    'lichess-broadcaster://broadcast/fide-grand-swiss-2025--open/round-1/xSCoiNg0',
  );
});

test('throws on invalid URL', () => {
  assert.throws(() => broadcasterDeepLink('invalid-url'), /TypeError: Invalid URL/);
});
