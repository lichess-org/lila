import type { Pieces } from '@lichess-org/chessground/types';
import assert from 'node:assert/strict';
import { test } from 'node:test';

import { commands } from '../src/nvui/command';

const pieces: Pieces = new Map([
  ['a1', { color: 'white', role: 'king' }],
  ['a2', { color: 'white', role: 'queen' }],
  ['b1', { color: 'white', role: 'knight' }],
  ['b2', { color: 'white', role: 'knight' }],
]);

test('piece command', () => {
  assert.strictEqual(commands().piece.apply('p Q', pieces, 'anna'), 'nvui.whiteQueen: anna 2');
  assert.strictEqual(commands().piece.apply('p K', pieces, 'san'), 'nvui.whiteKing: a1');
  assert.strictEqual(commands().piece.apply('p N', pieces, 'san'), 'nvui.whiteKnight: b1, b2');
  assert.strictEqual(commands().piece.apply('p N', pieces, 'nato'), 'nvui.whiteKnight: bravo 1, bravo 2');
  assert.strictEqual(commands().piece.apply('p b', pieces, 'san'), 'nvui.blackBishop: site.none');

  assert.strictEqual(commands().piece.apply('p X', pieces, 'san'), undefined);
  assert.strictEqual(commands().piece.apply('p |', pieces, 'san'), undefined);
});

test('scan command', () => {
  assert.strictEqual(commands().scan.apply('s a', pieces, 'san'), 'a1 nvui.whiteKing, a2 nvui.whiteQueen');
  assert.strictEqual(commands().scan.apply('s 1', pieces, 'san'), 'a1 nvui.whiteKing, b1 nvui.whiteKnight');

  assert.strictEqual(commands().scan.apply('s x', pieces, 'san'), undefined);
  assert.strictEqual(commands().scan.apply('s 9', pieces, 'san'), undefined);
});
