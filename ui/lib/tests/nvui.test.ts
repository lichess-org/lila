import { describe, expect, test } from 'vitest';
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

    // test pawn takes
    expect(inputToMove('bxc4', fen, cg)).toBe('b3c4');
    expect(inputToMove('bXc4', fen, cg)).toBe('b3c4');
    expect(inputToMove('bXC4', fen, cg)).toBe('b3c4');
    expect(inputToMove('bxC4', fen, cg)).toBe('b3c4');

    // test bishop takes
    expect(inputToMove('Bxc4', fen, cg)).toBe('d3c4');
    expect(inputToMove('BXc4', fen, cg)).toBe('d3c4');
    expect(inputToMove('BXC4', fen, cg)).toBe('d3c4');
    expect(inputToMove('BxC4', fen, cg)).toBe('d3c4');
  });

  test('mixed case promotions', async () => {
    const fen = '8/2P4k/8/8/8/8/7K/8 w - - 0 1';
    const cg = Chessground(document.createElement('div'), {
      fen,
      movable: {
        dests: new Map([['c7', ['c8']]]),
      },
    });

    // san
    expect(inputToMove('c8', fen, cg)).toBe('c7c8q');
    expect(inputToMove('C8', fen, cg)).toBe('c7c8q');
    expect(inputToMove('c8=R', fen, cg)).toBe('c7c8r');
    expect(inputToMove('C8=N', fen, cg)).toBe('c7c8n');
    // uci
    expect(inputToMove('c7c8b', fen, cg)).toBe('c7c8b');
    expect(inputToMove('C7C8B', fen, cg)).toBe('c7c8b');
    expect(inputToMove('c7c8B', fen, cg)).toBe('c7c8b');
  });

  test('mixed case crazyhouse drops', async () => {
    const fen = '8/2P4k/8/8/8/8/7K/8[] b - - 0 1';
    const cg = Chessground(document.createElement('div'), {
      fen,
      movable: {
        dests: new Map(),
      },
    });

    expect(inputToMove('q@c3', fen, cg)).toStrictEqual({ key: 'c3', role: 'queen' });
    expect(inputToMove('Q@c3', fen, cg)).toStrictEqual({ key: 'c3', role: 'queen' });
    expect(inputToMove('Q@C3', fen, cg)).toStrictEqual({ key: 'c3', role: 'queen' });
    expect(inputToMove('q@C3', fen, cg)).toStrictEqual({ key: 'c3', role: 'queen' });
  });
});
