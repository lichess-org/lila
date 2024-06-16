import { describe, expect, test } from 'vitest';
import { movePattern, userPattern } from '../src/richText';

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
});
