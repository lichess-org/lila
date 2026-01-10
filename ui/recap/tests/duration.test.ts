import { test } from 'node:test';
import assert from 'node:assert/strict';
import { formatDuration } from '../src/util';

test('singular and plural time units', () => {
  assert.strictEqual(formatDuration(0), 'site.nbHours(0)<br>site.nbMinutes(0)');
  assert.strictEqual(formatDuration(1), 'site.nbHours(0)<br>site.nbMinutes(0)');
  assert.strictEqual(formatDuration(2), 'site.nbHours(0)<br>site.nbMinutes(0)');

  assert.strictEqual(formatDuration(60), 'site.nbHours(0)<br>site.nbMinutes(1)');
  assert.strictEqual(formatDuration(120), 'site.nbHours(0)<br>site.nbMinutes(2)');

  assert.strictEqual(formatDuration(60 * 60 * 1), 'site.nbHours(1)<br>site.nbMinutes(0)');
  assert.strictEqual(formatDuration(60 * 60 * 2), 'site.nbHours(2)<br>site.nbMinutes(0)');

  assert.strictEqual(formatDuration(60 * 60 * 1 + 60 * 1), 'site.nbHours(1)<br>site.nbMinutes(1)');
  assert.strictEqual(formatDuration(60 * 60 * 1 + 60 * 2), 'site.nbHours(1)<br>site.nbMinutes(2)');
  assert.strictEqual(formatDuration(60 * 60 * 2 + 60 * 1), 'site.nbHours(2)<br>site.nbMinutes(1)');
  assert.strictEqual(formatDuration(60 * 60 * 2 + 60 * 2), 'site.nbHours(2)<br>site.nbMinutes(2)');

  assert.strictEqual(formatDuration(60 * 60 * 24 * 1), 'site.nbDays(1)<br>site.nbHours(0)');
  assert.strictEqual(formatDuration(60 * 60 * 24 * 2), 'site.nbDays(2)<br>site.nbHours(0)');

  assert.strictEqual(
    formatDuration(60 * 60 * 24 * 1 + 60 * 60 * 1 + 60 * 1),
    'site.nbDays(1)<br>site.nbHours(1)',
  );
  assert.strictEqual(
    formatDuration(60 * 60 * 24 * 2 + 60 * 60 * 2 + 60 * 2),
    'site.nbDays(2)<br>site.nbHours(2)',
  );
});
