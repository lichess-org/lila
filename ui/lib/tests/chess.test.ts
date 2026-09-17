import assert from 'node:assert/strict';
import { describe } from 'node:test';

import { each } from '../../.test/helpers.mts';
import { isFiftyMoveDraw, isFiftyMoves } from '../src/game/chess';

const fenWithClock = (halfmoves: number): FEN =>
  `8/p4p2/1p2p1p1/r1k1P1Pp/P1P2P1P/R1K5/8/8 b - - ${halfmoves} 81`;

describe('fifty-move rule', () => {
  each<[VariantKey, FEN, boolean]>([
    ['standard', 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1', false],
    ['standard', fenWithClock(99), false],
    ['standard', fenWithClock(100), true],
    ['standard', fenWithClock(150), true],
    ['atomic', fenWithClock(100), true],
    ['threeCheck', fenWithClock(100), true],
    ['chess960', fenWithClock(100), true],
    // scalachess overrides Crazyhouse.fiftyMoves to false
    ['crazyhouse', 'r1bqkb1r/pppppppp/2n2n2/8/8/2N2N2/PPPPPPPP/R1BQKB1R[] w KQkq - 100 51', false],
    ['standard', '8/8/8/8/8/8/8/8 w - -', false],
    // clock is 100, but this position is checkmate — predicate stays true
    ['standard', '7k/6Q1/5K2/8/8/8/8/8 b - - 100 50', true],
  ])('isFiftyMoves', (variant, fen, expected) => assert.strictEqual(isFiftyMoves(variant, fen), expected));
});

describe('fifty-move draw vs board outcome', () => {
  const mateAt100: FEN = '7k/6Q1/5K2/8/8/8/8/8 b - - 100 50';
  each<[string, VariantKey, FEN, { winner?: Color } | undefined, boolean]>([
    ['no outcome is a draw', 'standard', fenWithClock(100), undefined, true],
    ['checkmate beats the clock', 'standard', mateAt100, { winner: 'white' }, false],
    ['stalemate beats the clock', 'standard', fenWithClock(100), { winner: undefined }, false],
    ['crazyhouse never draws this way', 'crazyhouse', fenWithClock(100), undefined, false],
  ])('isFiftyMoveDraw', (_name, variant, fen, outcome, expected) =>
    assert.strictEqual(isFiftyMoveDraw(variant, fen, outcome), expected),
  );
});
