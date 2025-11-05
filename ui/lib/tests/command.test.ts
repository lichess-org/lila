import { before, test } from 'node:test';
import assert from 'node:assert/strict';
import { commands } from '../src/nvui/command';
import type { Pieces } from '@lichess-org/chessground/types';

const pieces: Pieces = new Map();
pieces.set('a1', { color: 'white', role: 'king' });
pieces.set('a2', { color: 'white', role: 'queen' });
pieces.set('b1', { color: 'white', role: 'knight' });
pieces.set('b2', { color: 'white', role: 'knight' });

type RecursivePartial<T> = { [P in keyof T]?: RecursivePartial<T[P]> };

before(() => {
  const i18n: RecursivePartial<I18n> = {
    site: { none: 'None' },
    nvui: new Proxy(
      {
        whiteKing: 'white king',
        whiteQueen: 'white queen',
        whiteKnight: 'white knight',
        blackBishop: 'black bishop',
      },
      {
        get: (target, prop: string | symbol) => (prop in target ? (target as any)[prop] : () => {}),
      },
    ),
  };
  (globalThis as any).i18n = i18n;
});

test('piece command', () => {
  assert.strictEqual(commands().piece.apply('p Q', pieces, 'anna'), 'white queen: anna 2');
  assert.strictEqual(commands().piece.apply('p K', pieces, 'san'), 'white king: a1');
  assert.strictEqual(commands().piece.apply('p N', pieces, 'san'), 'white knight: b1, b2');
  assert.strictEqual(commands().piece.apply('p N', pieces, 'nato'), 'white knight: bravo 1, bravo 2');
  assert.strictEqual(commands().piece.apply('p b', pieces, 'san'), 'black bishop: None');

  assert.strictEqual(commands().piece.apply('p X', pieces, 'san'), undefined);
  assert.strictEqual(commands().piece.apply('p |', pieces, 'san'), undefined);
});

test('scan command', () => {
  assert.strictEqual(commands().scan.apply('s a', pieces, 'san'), 'a1 white king, a2 white queen');
  assert.strictEqual(commands().scan.apply('s 1', pieces, 'san'), 'a1 white king, b1 white knight');

  assert.strictEqual(commands().scan.apply('s x', pieces, 'san'), undefined);
  assert.strictEqual(commands().scan.apply('s 9', pieces, 'san'), undefined);
});
