import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { bishopOnColor, expandFen, insufficientMaterial } from '../src/game/view/status';
import { each } from '../../.test/helpers.mts';

describe('expand fen', () => {
  test('starting position', () =>
    assert.strictEqual(
      expandFen('rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1'),
      'rnbqkbnrpppppppp11111111111111111111111111111111PPPPPPPPRNBQKBNR',
    ));

  test('middlegame position', () =>
    assert.strictEqual(
      expandFen('r2q1rk1/p3ppbp/2pp1np1/2n5/2P3b1/1P1BPN2/PB1N1PPP/2RQ1RK1 w HAhq - 0 1'),
      'r11q1rk1p111ppbp11pp1np111n1111111P111b11P1BPN11PB1N1PPP11RQ1RK1',
    ));
});

describe('bishop on color', () => {
  test('bishop on square', () => {
    assert.strictEqual(bishopOnColor(expandFen('B7/8/8/8/8/8/8/8 w - - 0 1'), 0), true);
    assert.strictEqual(bishopOnColor(expandFen('2B5/8/8/8/8/8/8/8 w - - 0 1'), 0), true);
    assert.strictEqual(bishopOnColor(expandFen('3B4/8/8/8/8/8/8/8 w - - 0 1'), 1), true);
    assert.strictEqual(bishopOnColor(expandFen('2BB4/8/8/8/8/8/8/8 w - - 0 1'), 1), true);
  });

  test('no bishops on black squares', () => {
    assert.strictEqual(bishopOnColor(expandFen('B7/8/8/8/8/8/8/8 w - - 0 1'), 1), false);
    assert.strictEqual(bishopOnColor(expandFen('2B5/8/8/8/8/8/8/8 w - - 0 1'), 1), false);
    assert.strictEqual(bishopOnColor(expandFen('5K2/8/8/1B6/8/k7/6b1/8 w - - 0 39'), 1), false);
  });
});

describe('test insufficient material', () => {
  test('K vs K', () =>
    assert.strictEqual(insufficientMaterial('standard', '4k3/8/8/8/8/8/8/4K3 w - - 0 1'), true));

  test('KB vs K', () =>
    assert.strictEqual(insufficientMaterial('standard', '4k3/8/8/8/8/8/8/4KB2 w - - 0 1'), true));

  test('KBB vs K (same color bishops)', () =>
    assert.strictEqual(insufficientMaterial('standard', '4k3/8/8/8/8/8/6B1/4K2B w - - 0 1'), true));

  test('KB vs KB (same color bishops)', () =>
    assert.strictEqual(insufficientMaterial('standard', 'k7/8/1b6/8/8/8/1B6/K7 w - - 0 1'), true));
});

describe('should not be insufficient material', () => {
  each<[VariantKey]>([
    ['horde'],
    ['kingOfTheHill'],
    ['racingKings'],
    ['crazyhouse'],
    ['atomic'],
    ['antichess'],
    ['threeCheck'],
  ])('variant %s', variant =>
    assert.strictEqual(
      insufficientMaterial(variant, 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1'),
      false,
    ),
  );

  test('pawn is never insufficient material', () =>
    assert.strictEqual(insufficientMaterial('standard', '4k3/8/8/8/8/8/7P/4K3 w - - 0 1'), false));

  test('rook is never insufficient material', () =>
    assert.strictEqual(insufficientMaterial('standard', '4k3/8/8/8/8/8/7R/4K3 w - - 0 1'), false));

  test('queen is never insufficient material', () =>
    assert.strictEqual(insufficientMaterial('standard', '4k3/8/8/8/8/8/7Q/4K3 w - - 0 1'), false));

  test('KBB vs K (diff color bishops)', () => {
    assert.strictEqual(insufficientMaterial('standard', '8/8/1B6/8/1KB5/8/2k5/8 b - - 100 103'), false);
    assert.strictEqual(insufficientMaterial('standard', '8/8/1B6/8/1KB5/8/2k5/8'), false);
  });

  test('KB vs KN', () =>
    assert.strictEqual(insufficientMaterial('standard', 'kn6/8/8/8/8/8/8/KB6 w - - 0 1'), false));

  test('KB vs KB (diff color bishops)', () =>
    assert.strictEqual(insufficientMaterial('standard', 'k7/1b6/8/8/8/8/1B6/K7 w - - 0 1'), false));
});

describe('knight rules', () => {
  test('KN vs K', () =>
    assert.strictEqual(insufficientMaterial('standard', 'k7/8/1n6/8/8/8/8/K7 w - - 0 1'), true));

  test('KNN vs K', () =>
    assert.strictEqual(insufficientMaterial('standard', 'k7/8/1nn5/8/8/8/8/K7 w - - 0 1'), false));
});

describe('scalachess fens from AutodrawTest.scala', () => {
  each<[string]>([['5K2/8/8/1B6/8/k7/6b1/8 w - - 0 39']])('should detect insufficient material', fen => {
    assert.strictEqual(insufficientMaterial('standard', fen), true);
  });

  each<[string]>([
    ['1n2k1n1/8/8/8/8/8/8/4K3 w - - 0 1'],
    ['7K/5k2/7P/6n1/8/8/8/8 b - - 0 40'],
    ['1b1b3K/8/5k1P/8/8/8/8/8 b - - 0 40'],
    ['b2b3K/8/5k1Q/8/8/8/8/8 b - -'],
    ['1b1b3K/8/5k1Q/8/8/8/8/8 b - -'],
    ['8/8/5N2/8/6p1/8/5K1p/7k w - - 0 37'],
    ['8/8/8/4N3/4k1p1/6K1/8/3b4 w - - 5 59'],
    ['8/8/3Q4/2bK4/B7/8/8/k7 b - - 0 67'],
  ])('should not detect insufficient material', fen => {
    assert.strictEqual(insufficientMaterial('standard', fen), false);
  });
});
