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
    nvui: {
      whiteKings: Object.assign((quantity: number) => (quantity == 1 ? 'white king' : 'white kings')),
      whiteQueens: Object.assign((quantity: number) => (quantity == 1 ? 'white queen' : 'white queens')),
      whiteKnights: Object.assign((quantity: number) => (quantity == 1 ? 'white knight' : 'white knights')),
      blackBishops: Object.assign((quantity: number) => (quantity == 1 ? 'black bishop' : 'black bishops')),
    },
  };
  vi.stubGlobal('i18n', i18n);
});

test('piece command', () => {
  expect(commands(i18n).piece.apply('p Q', pieces, 'anna')).toBe('white queen: anna 2');
  expect(commands(i18n).piece.apply('p K', pieces, 'san')).toBe('white king: a1');
  expect(commands(i18n).piece.apply('p N', pieces, 'san')).toBe('white knights: b1, b2');
  expect(commands(i18n).piece.apply('p N', pieces, 'nato')).toBe('white knights: bravo 1, bravo 2');
  expect(commands(i18n).piece.apply('p b', pieces, 'san')).toBe('black bishops: None');

  expect(commands(i18n).piece.apply('p X', pieces, 'san')).toBeUndefined();
  expect(commands(i18n).piece.apply('p |', pieces, 'san')).toBeUndefined();
});

test('scan command', () => {
  expect(commands(i18n).scan.apply('s a', pieces, 'san')).toBe('a1 white king, a2 white queen');
  expect(commands(i18n).scan.apply('s 1', pieces, 'san')).toBe('a1 white king, b1 white knight');

  expect(commands(i18n).scan.apply('s x', pieces, 'san')).toBeUndefined();
  expect(commands(i18n).scan.apply('s 9', pieces, 'san')).toBeUndefined();
});
