import assert from 'node:assert/strict';
import { test } from 'node:test';

import { practicePovColor } from '../src/practice/practiceColor';

test('uses the visible board color for regular variants', () => {
  assert.equal(practicePovColor('standard', 'black', 'white'), 'white');
  assert.equal(practicePovColor('horde', 'white', 'black'), 'black');
});

test('uses the player color for Racing Kings practice', () => {
  assert.equal(practicePovColor('racingKings', 'black', 'white'), 'black');
  assert.equal(practicePovColor('racingKings', 'white', 'white'), 'white');
});
