import assert from 'node:assert/strict';
import { describe } from 'node:test';

import { each } from '../../.test/helpers.mts';
import { isFiftyMoves } from '../src/game/chess';

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
  ])('isFiftyMoves', (variant, fen, expected) => assert.strictEqual(isFiftyMoves(variant, fen), expected));
});
