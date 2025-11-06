import { test } from 'node:test';
import assert from 'node:assert/strict';
import { formatDuration } from '../src/util';

test('singular and plural time units', () => {
  assert.strictEqual(formatDuration(0), '0 hours<br>0 minutes');
  assert.strictEqual(formatDuration(1), '0 hours<br>0 minutes');
  assert.strictEqual(formatDuration(2), '0 hours<br>0 minutes');

  assert.strictEqual(formatDuration(60), '0 hours<br>1 minute');
  assert.strictEqual(formatDuration(120), '0 hours<br>2 minutes');

  assert.strictEqual(formatDuration(60 * 60 * 1), '1 hour<br>0 minutes');
  assert.strictEqual(formatDuration(60 * 60 * 2), '2 hours<br>0 minutes');

  assert.strictEqual(formatDuration(60 * 60 * 1 + 60 * 1), '1 hour<br>1 minute');
  assert.strictEqual(formatDuration(60 * 60 * 1 + 60 * 2), '1 hour<br>2 minutes');
  assert.strictEqual(formatDuration(60 * 60 * 2 + 60 * 1), '2 hours<br>1 minute');
  assert.strictEqual(formatDuration(60 * 60 * 2 + 60 * 2), '2 hours<br>2 minutes');

  assert.strictEqual(formatDuration(60 * 60 * 24 * 1), '1 day<br>0 hours');
  assert.strictEqual(formatDuration(60 * 60 * 24 * 2), '2 days<br>0 hours');

  assert.strictEqual(formatDuration(60 * 60 * 24 * 1 + 60 * 60 * 1 + 60 * 1), '1 day<br>1 hour');
  assert.strictEqual(formatDuration(60 * 60 * 24 * 2 + 60 * 60 * 2 + 60 * 2), '2 days<br>2 hours');
});
