import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { enhance, EnhanceOpts, movePattern, userPattern } from '../src/richText';
import { each } from '../../.test/helpers.mts';

describe('test regex patterns', () => {
  test('username mentions', () => {
    assert.deepStrictEqual('@foo'.match(userPattern), ['@foo']);
    assert.deepStrictEqual('@foo-'.match(userPattern), ['@foo-']);
    assert.deepStrictEqual('@__foo'.match(userPattern), ['@__foo']);
    assert.deepStrictEqual('@Foo'.match(userPattern), ['@Foo']);
  });

  each<[string]>([
    ['1.e4'],
    ['1. e4'],
    ['5...Nf6'],
    ['5... Nf6'],
    ['12.♔d1'],
    ['10.O-O-O'],
    ['10.o-o-o'],
    ['10.0-0-0'],
  ])('moves', move => {
    assert.deepStrictEqual(move.match(movePattern), [move]);
  });

  test('move with comment', () => {
    assert.deepStrictEqual('I considered 34. f7+ instead'.match(movePattern), ['34. f7+']);
  });

  test('not a move', () => {
    assert.strictEqual('4.m3'.match(movePattern), null);
  });

  test('board links', () => {
    const opts: EnhanceOpts = { boards: true };
    assert.strictEqual(enhance('board 1', opts), '<a data-board="1">board 1</a>');
    assert.strictEqual(enhance('board 100', opts), '<a data-board="100">board 100</a>');
    assert.strictEqual(enhance('game 50', opts), '<a data-board="50">game 50</a>');
    assert.strictEqual(enhance('game 64', opts), '<a data-board="64">game 64</a>');
    assert.strictEqual(enhance('game 65', opts), '<a data-board="65">game 65</a>');
    assert.strictEqual(enhance('game 100', opts), '<a data-board="100">game 100</a>');
  });

  test('boards out of range should not be linked', () => {
    const opts: EnhanceOpts = { boards: true };
    assert.strictEqual(enhance('board 0', opts), 'board 0');
    assert.strictEqual(enhance('board 101', opts), 'board 101');
    assert.strictEqual(enhance('board 1000', opts), 'board 1000');
    assert.strictEqual(enhance('board 9999', opts), 'board 9999');
  });
});
