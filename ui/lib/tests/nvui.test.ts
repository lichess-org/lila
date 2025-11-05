import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { inputToMove } from '../src/nvui/chess';
import { Chessground } from '@lichess-org/chessground';

describe('nvui move inputs', () => {
  test('mixed case bishop or pawn takes', async () => {
    const fen = '7k/8/8/8/2p5/1P1B4/8/7K w - - 0 1';
    const cg = Chessground(document.createElement('div') as any, {
      fen,
      movable: {
        dests: new Map<Key, Key[]>([
          ['b3', ['b4', 'c4']],
          ['d3', ['c2', 'c4', 'd4']],
        ]),
      },
    });
  });

  test('drop piece inputs', () => {
    const fen = '8/2P4k/8/8/8/8/7K/8[] b - - 0 1';
    const cg = Chessground(document.createElement('div') as any, {
      fen,
      movable: { dests: new Map() },
    });

    assert.deepEqual(inputToMove('q@c3', fen, cg), { key: 'c3', role: 'queen' });
    assert.deepEqual(inputToMove('Q@c3', fen, cg), { key: 'c3', role: 'queen' });
    assert.deepEqual(inputToMove('Q@C3', fen, cg), { key: 'c3', role: 'queen' });
    assert.deepEqual(inputToMove('q@C3', fen, cg), { key: 'c3', role: 'queen' });
  });
});
