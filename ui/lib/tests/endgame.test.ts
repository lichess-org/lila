import assert from 'node:assert/strict';
import { test } from 'node:test';

import { endgameShapes } from '../src/game/endgame';
import type { StatusName } from '../src/game/status';

const origs = (fen: FEN, winner: Color | undefined, status: StatusName) =>
  endgameShapes(fen, winner, status).map(shape => shape.orig);

test('glyph goes on the king when there is one', () => {
  assert.deepStrictEqual(origs('4k3/8/8/8/8/8/8/Q3K3 w - - 0 1', 'black', 'resign'), ['e1']);
});

test('glyph falls back to the strongest piece without a king', () => {
  assert.deepStrictEqual(origs('8/8/8/8/8/8/8/6N1 b - - 0 28', 'black', 'variantEnd'), ['g1']);
  assert.deepStrictEqual(origs('8/8/8/8/8/8/2P5/1NR5 b - - 0 40', 'black', 'variantEnd'), ['c1']);
  assert.deepStrictEqual(origs('8/8/8/8/8/8/2P5/1N6 b - - 0 40', 'black', 'variantEnd'), ['b1']);
});

test('draw glyphs show for a side without a king', () => {
  assert.deepStrictEqual(origs('4k3/8/8/8/8/8/PPPPPPPP/8 w - - 0 1', undefined, 'stalemate'), ['a2', 'e8']);
});

test('no glyph for a side without pieces', () => {
  assert.deepStrictEqual(origs('8/8/8/8/8/8/8/6N1 b - - 0 28', 'white', 'variantEnd'), []);
});
