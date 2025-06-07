import { describe, expect, test, vi } from 'vitest';
import { inputToMove } from '../src/nvui/chess';
import { Chessground } from '@lichess-org/chessground';

describe('test input moves', () => {
  test('case-sensitive captures', async () => {
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

    // test pawn takes
    expect(inputToMove('bxc4', fen, cg)).toBe('b3c4');
    expect(inputToMove('bXc4', fen, cg)).toBe('b3c4');
    expect(inputToMove('bXC4', fen, cg)).toBe('b3c4');

    // test bishop takes
    expect(inputToMove('Bxc4', fen, cg)).toBe('d3c4');
  });
});
