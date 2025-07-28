import { beforeAll, expect, test, vi } from 'vitest';
import { commands } from '../src/nvui/command';
import type { Pieces } from '@lichess-org/chessground/types';

const pieces: Pieces = new Map();
pieces.set('a1', { color: 'white', role: 'king' });
pieces.set('a2', { color: 'white', role: 'queen' });
pieces.set('b1', { color: 'white', role: 'knight' });
pieces.set('b2', { color: 'white', role: 'knight' });

type RecursivePartial<T> = {
  [P in keyof T]?: RecursivePartial<T[P]>;
};

beforeAll(() => {
  const i18n: RecursivePartial<I18n> = {
    site: {
      none: 'None',
    },
    nvui: new Proxy(
      {
        whiteKing: 'white king',
        whiteQueen: 'white queen',
        whiteKnight: 'white knight',
        blackBishop: 'black bishop',
      },
      {
        get: (target, prop: string) => (prop in target ? target[prop as keyof typeof target] : () => {}),
      },
    ),
  };
  vi.stubGlobal('i18n', i18n);
});

test('piece command', () => {
  expect(commands().piece.apply('p Q', pieces, 'anna')).toBe('white queen: anna 2');
  expect(commands().piece.apply('p K', pieces, 'san')).toBe('white king: a1');
  expect(commands().piece.apply('p N', pieces, 'san')).toBe('white knight: b1, b2');
  expect(commands().piece.apply('p N', pieces, 'nato')).toBe('white knight: bravo 1, bravo 2');
  expect(commands().piece.apply('p b', pieces, 'san')).toBe('black bishop: None');

  expect(commands().piece.apply('p X', pieces, 'san')).toBeUndefined();
  expect(commands().piece.apply('p |', pieces, 'san')).toBeUndefined();
});

test('scan command', () => {
  expect(commands().scan.apply('s a', pieces, 'san')).toBe('a1 white king, a2 white queen');
  expect(commands().scan.apply('s 1', pieces, 'san')).toBe('a1 white king, b1 white knight');

  expect(commands().scan.apply('s x', pieces, 'san')).toBeUndefined();
  expect(commands().scan.apply('s 9', pieces, 'san')).toBeUndefined();
});
