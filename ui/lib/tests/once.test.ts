import { describe, test, mock } from 'node:test';
import assert from 'node:assert/strict';
import { once } from '../src/storage';

describe('test once', () => {
  test('once', async () => {
    assert.equal(once('foo'), true);

    assert.equal(once('foo'), false);
    assert.equal(once('foo'), false);

    mock.timers.enable({ apis: ['Date'], now: 1 });

    assert.equal(once('secs', { seconds: 1 }), true);
    assert.equal(once('secs', { seconds: 1 }), false, 'ohnoes');

    mock.timers.tick(1050);
    assert.equal(once('secs', { seconds: 1 }), true);

    mock.timers.reset();
  });
});
