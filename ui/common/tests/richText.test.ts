import { describe, expect, test } from 'vitest';
import { enhance, EnhanceOpts, movePattern, userPattern } from '../src/richText';

describe('test regex patterns', () => {
  test('username mentions', () => {
    expect('@foo'.match(userPattern)).toStrictEqual(['@foo']);
    expect('@foo-'.match(userPattern)).toStrictEqual(['@foo-']);
    expect('@__foo'.match(userPattern)).toStrictEqual(['@__foo']);
    expect('@Foo'.match(userPattern)).toStrictEqual(['@Foo']);
  });

  test.each([
    ['1.e4'],
    ['1. e4'],
    ['5...Nf6'],
    ['5... Nf6'],
    ['12.♔d1'],
    ['10.O-O-O'],
    ['10.o-o-o'],
    ['10.0-0-0'],
  ])('moves', move => {
    expect(move.match(movePattern)).toStrictEqual([move]);
  });

  test('move with comment', () => {
    expect('I considered 34. f7+ instead'.match(movePattern)).toStrictEqual(['34. f7+']);
  });

  test('not a move', () => {
    expect('4.m3'.match(movePattern)).toBeNull();
  });

  test('board links', () => {
    const opts: EnhanceOpts = {
      boards: true,
    };
    expect(enhance('board 1', opts)).toBe('<a data-board="1">board 1</a>');
    expect(enhance('board 100', opts)).toBe('<a data-board="100">board 100</a>');
    expect(enhance('game 50', opts)).toBe('<a data-board="50">game 50</a>');
    expect(enhance('game 64', opts)).toBe('<a data-board="64">game 64</a>');
    expect(enhance('game 65', opts)).toBe('<a data-board="65">game 65</a>');
    expect(enhance('game 100', opts)).toBe('<a data-board="100">game 100</a>');
  });

  test('boards out of range should not be linked', () => {
    const opts: EnhanceOpts = {
      boards: true,
    };
    expect(enhance('board 0', opts)).toBe('board 0');
    expect(enhance('board 101', opts)).toBe('board 101');
    expect(enhance('board 1000', opts)).toBe('board 1000');
    expect(enhance('board 9999', opts)).toBe('board 9999');
  });
});
