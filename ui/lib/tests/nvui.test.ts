import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { inputToMove } from '../src/nvui/chess';
import { Chessground } from '@lichess-org/chessground';

describe('nvui move inputs', () => {
  test('mixed case bishop or pawn takes', async () => {
    const fen = '7k/8/8/8/2p5/1P1B4/8/7K w - - 0 1';
    const cg = Chessground(document.createElement('div'), {
      fen,
      movable: {
        dests: new Map([
          ['b3', ['b4', 'c4']],
          ['d3', ['c2', 'c4', 'd4']],
        ]),
      },
    });

    // pawn takes
    assert.equal(inputToMove('bxc4', fen, cg), 'b3c4');
    assert.equal(inputToMove('bXc4', fen, cg), 'b3c4');
    assert.equal(inputToMove('bXC4', fen, cg), 'b3c4');
    assert.equal(inputToMove('bxC4', fen, cg), 'b3c4');

    // bishop takes
    assert.equal(inputToMove('Bxc4', fen, cg), 'd3c4');
    assert.equal(inputToMove('BXc4', fen, cg), 'd3c4');
    assert.equal(inputToMove('BXC4', fen, cg), 'd3c4');
    assert.equal(inputToMove('BxC4', fen, cg), 'd3c4');
  });

  test('mixed case promotions', async () => {
    const fen = '8/2P4k/8/8/8/8/7K/8 w - - 0 1';
    const cg = Chessground(document.createElement('div'), {
      fen,
      movable: { dests: new Map([['c7', ['c8']]]) },
    });

    // san
    assert.equal(inputToMove('c8', fen, cg), 'c7c8q');
    assert.equal(inputToMove('C8', fen, cg), 'c7c8q');
    assert.equal(inputToMove('c8=R', fen, cg), 'c7c8r');
    assert.equal(inputToMove('C8=N', fen, cg), 'c7c8n');
    // uci
    assert.equal(inputToMove('c7c8b', fen, cg), 'c7c8b');
    assert.equal(inputToMove('C7C8B', fen, cg), 'c7c8b');
    assert.equal(inputToMove('c7c8B', fen, cg), 'c7c8b');
  });

  test('mixed case crazyhouse drops', async () => {
    const fen = '8/2P4k/8/8/8/8/7K/8[] b - - 0 1';
    const cg = Chessground(document.createElement('div'), {
      fen,
      movable: { dests: new Map() },
    });

    assert.deepEqual(inputToMove('q@c3', fen, cg), { key: 'c3', role: 'queen' });
    assert.deepEqual(inputToMove('Q@c3', fen, cg), { key: 'c3', role: 'queen' });
    assert.deepEqual(inputToMove('Q@C3', fen, cg), { key: 'c3', role: 'queen' });
    assert.deepEqual(inputToMove('q@C3', fen, cg), { key: 'c3', role: 'queen' });
  });
});
