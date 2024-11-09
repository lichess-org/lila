import { describe, expect, test } from 'vitest';
import { bishopOnColor, expandFen, insufficientMaterial } from '../src/view/status';

describe('expand fen', () => {
  test('starting position', () =>
    expect(expandFen('rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1')).toBe(
      'rnbqkbnrpppppppp11111111111111111111111111111111PPPPPPPPRNBQKBNR',
    ));
  test('middlegame position', () =>
    expect(expandFen('r2q1rk1/p3ppbp/2pp1np1/2n5/2P3b1/1P1BPN2/PB1N1PPP/2RQ1RK1 w HAhq - 0 1')).toBe(
      'r11q1rk1p111ppbp11pp1np111n1111111P111b11P1BPN11PB1N1PPP11RQ1RK1',
    ));
});

describe('bishop on color', () => {
  test('bishop on square', () => {
    expect(bishopOnColor(expandFen('B7/8/8/8/8/8/8/8 w - - 0 1'), 0)).toBe(true);
    expect(bishopOnColor(expandFen('2B5/8/8/8/8/8/8/8 w - - 0 1'), 0)).toBe(true);
    expect(bishopOnColor(expandFen('3B4/8/8/8/8/8/8/8 w - - 0 1'), 1)).toBe(true);
    expect(bishopOnColor(expandFen('2BB4/8/8/8/8/8/8/8 w - - 0 1'), 1)).toBe(true);
  });
  test('no bishops on black squares', () => {
    expect(bishopOnColor(expandFen('B7/8/8/8/8/8/8/8 w - - 0 1'), 1)).toBe(false);
    expect(bishopOnColor(expandFen('2B5/8/8/8/8/8/8/8 w - - 0 1'), 1)).toBe(false);
    expect(bishopOnColor(expandFen('5K2/8/8/1B6/8/k7/6b1/8 w - - 0 39'), 1)).toBe(false);
  });
});

describe('test insufficient material', () => {
  test('K vs K', () => expect(insufficientMaterial('standard', '4k3/8/8/8/8/8/8/4K3 w - - 0 1')).toBe(true));

  test('KB vs K', () =>
    expect(insufficientMaterial('standard', '4k3/8/8/8/8/8/8/4KB2 w - - 0 1')).toBe(true));

  test('KBB vs K (same color bishops)', () =>
    expect(insufficientMaterial('standard', '4k3/8/8/8/8/8/6B1/4K2B w - - 0 1')).toBe(true));

  test('KB vs KB (same color bishops)', () =>
    expect(insufficientMaterial('standard', 'k7/8/1b6/8/8/8/1B6/K7 w - - 0 1')).toBe(true));
});

describe('should not be insufficient material', () => {
  test.each<VariantKey[]>([
    ['horde'],
    ['kingOfTheHill'],
    ['racingKings'],
    ['crazyhouse'],
    ['atomic'],
    ['antichess'],
    ['threeCheck'],
  ])('variant %s', variant =>
    expect(insufficientMaterial(variant, 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1')).toBe(
      false,
    ),
  );

  test('pawn is never insufficient material', () =>
    expect(insufficientMaterial('standard', '4k3/8/8/8/8/8/7P/4K3 w - - 0 1')).toBe(false));

  test('rook is never insufficient material', () =>
    expect(insufficientMaterial('standard', '4k3/8/8/8/8/8/7R/4K3 w - - 0 1')).toBe(false));

  test('queen is never insufficient material', () =>
    expect(insufficientMaterial('standard', '4k3/8/8/8/8/8/7Q/4K3 w - - 0 1')).toBe(false));

  test('KBB vs K (diff color bishops)', () => {
    expect(insufficientMaterial('standard', '8/8/1B6/8/1KB5/8/2k5/8 b - - 100 103')).toBe(false);
    expect(insufficientMaterial('standard', '8/8/1B6/8/1KB5/8/2k5/8')).toBe(false);
  });

  test('KB vs KN', () =>
    expect(insufficientMaterial('standard', 'kn6/8/8/8/8/8/8/KB6 w - - 0 1')).toBe(false));

  test('KB vs KB (diff color bishops)', () =>
    expect(insufficientMaterial('standard', 'k7/1b6/8/8/8/8/1B6/K7 w - - 0 1')).toBe(false));
});

describe('knight rules', () => {
  test('KN vs K', () => expect(insufficientMaterial('standard', 'k7/8/1n6/8/8/8/8/K7 w - - 0 1')).toBe(true));
  test('KNN vs K', () =>
    expect(insufficientMaterial('standard', 'k7/8/1nn5/8/8/8/8/K7 w - - 0 1')).toBe(false));
});

describe('scalachess fens from AutodrawTest.scala', () => {
  test.each([['5K2/8/8/1B6/8/k7/6b1/8 w - - 0 39']])('should detect insufficient material', fen => {
    expect(insufficientMaterial('standard', fen)).toBe(true);
  });

  test.each([
    ['1n2k1n1/8/8/8/8/8/8/4K3 w - - 0 1'],
    ['7K/5k2/7P/6n1/8/8/8/8 b - - 0 40'],
    ['1b1b3K/8/5k1P/8/8/8/8/8 b - - 0 40'],
    ['b2b3K/8/5k1Q/8/8/8/8/8 b - -'],
    ['1b1b3K/8/5k1Q/8/8/8/8/8 b - -'],
    ['8/8/5N2/8/6p1/8/5K1p/7k w - - 0 37'],
    ['8/8/8/4N3/4k1p1/6K1/8/3b4 w - - 5 59'],
    ['8/8/3Q4/2bK4/B7/8/8/k7 b - - 0 67'],
  ])('should not detect insufficient material', fen => {
    expect(insufficientMaterial('standard', fen)).toBe(false);
  });
});
