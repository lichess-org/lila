import { test } from 'node:test';
import assert from 'node:assert/strict';
import { parseFen } from 'chessops/fen';
import { makeSquare } from 'chessops/util';
import { detectPins, detectUndefended, detectCheckable } from '../src/motif/boardAnalysis';

function runAnalysis(fen: string): string[] {
  const parsed = parseFen(fen);
  if ('error' in parsed) throw parsed.error;
  const { board, epSquare, castlingRights } = parsed.value;

  const pins = detectPins(board).map(p => `${makeSquare(p.pinned)}:pin`);
  const undefended = detectUndefended(board, epSquare).map(u => `${makeSquare(u.square)}:undefended`);
  const checkable = detectCheckable(board, epSquare, castlingRights).map(
    s => `${makeSquare(s.king)}:checkable`,
  );

  return [...pins, ...undefended, ...checkable].sort();
}

test('Pin: Absolute', () => {
  const fen = '4k3/4n3/8/8/8/4R3/8/K7 w - - 0 1';
  const expected = ['e8:checkable', 'e7:pin'].sort();

  assert.deepEqual(runAnalysis(fen), expected);
});

test('Pin: Relative', () => {
  const fen = '4q2k/4n2p/8/8/8/4R3/P7/K7 w - - 0 1';
  const expected = ['e7:pin'];

  assert.deepEqual(runAnalysis(fen), expected);
});

test('Pin: Trade', () => {
  const fen = 'k2q1b2/8/3n4/8/1B6/8/7P/7K w - - 0 1';
  const expected: string[] = [];

  assert.deepEqual(runAnalysis(fen), expected);
});

test('Checkable: Castling', () => {
  const fen = '8/8/8/8/8/8/3PPP2/k3K2R w K - 0 1';
  const expected = ['a1:checkable'];

  assert.deepEqual(runAnalysis(fen), expected);
});

test('Checkable: En passant', () => {
  const fen = '7k/8/8/8/4Pp2/8/3K4/8 b - e3 0 1';
  const expected = ['d2:checkable'];

  assert.deepEqual(runAnalysis(fen), expected);
});

test('Checkable: Promotion', () => {
  const fen = '4k3/2P5/8/8/8/8/8/2K5 w - - 0 1';
  const expected = ['e8:checkable'];

  assert.deepEqual(runAnalysis(fen), expected);
});

test('Checkable: Underpromotion', () => {
  const fen = '8/2P1k3/8/8/8/8/8/2K5 w - - 0 1';
  const expected = ['e7:checkable'];

  assert.deepEqual(runAnalysis(fen), expected);
});

test('Undefended: Fork', () => {
  const fen = '8/5k2/2p1n3/3P4/8/8/8/2K5 w - - 0 1';
  const expected = ['f7:checkable', 'c6:undefended', 'e6:undefended', 'd5:undefended'].sort();

  assert.deepEqual(runAnalysis(fen), expected);
});

test('Undefended: Underdefended', () => {
  const fen = '6k1/8/8/r7/1b6/P7/1B5P/7K w - - 0 1';
  const expected = ['b4:undefended', 'a3:undefended'].sort();

  assert.deepEqual(runAnalysis(fen), expected);
});

test('Undefended: Losing trade', () => {
  const fen = '7k/6p1/5p2/r7/1b6/P7/1Q5P/2B4K w - - 0 1';
  const expected = ['b4:undefended', 'a3:undefended'].sort();

  assert.deepEqual(runAnalysis(fen), expected);
});

test('Undefended: Order of trades', () => {
  const fen = '6rk/6pp/5p2/r7/1b6/P3Q3/1B5P/7K w - - 0 1';
  const expected = ['b4:undefended'];

  assert.deepEqual(runAnalysis(fen), expected);
});

test('Undefended: Absolute pin', () => {
  const fen = '3k4/3b4/8/1B6/1b6/2N5/1P6/4K3 w - - 0 1';
  const expected = ['b5:undefended', 'c3:pin', 'e1:checkable'].sort();
  assert.deepEqual(runAnalysis(fen), expected);
});

test('Undefended: Pawn hanging directly and via en passant', () => {
  const fen = '7k/8/8/3pPp2/8/8/6B1/6K1 w - d6 0 1';
  const expected = ['d5:undefended', 'd6:undefended'].sort();
  assert.deepEqual(runAnalysis(fen), expected);
});

test('Undefended: Pawn hanging directly but protected from en passant', () => {
  const fen = '5b1k/8/8/3pPp2/8/8/6B1/6K1 w - d6 0 1';
  const expected = ['d5:undefended', 'g1:checkable'].sort();
  assert.deepEqual(runAnalysis(fen), expected);
});

test('Undefended: Pawn hanging via en passant but not directly', () => {
  const fen = '6bk/8/8/3pPp2/8/8/6B1/6K1 w - d6 0 1';
  const expected = ['d6:undefended'];
  assert.deepEqual(runAnalysis(fen), expected);
});

test('Undefended: En passant rook xray', () => {
  const fen = '5b1k/8/8/3pPp2/8/8/8/3R2K1 w - d6 0 1';
  const expected = ['d6:undefended', 'd5:undefended', 'g1:checkable'].sort();
  assert.deepEqual(runAnalysis(fen), expected);
});

test('Undefended: Attacker also pins defender', () => {
  const fen = '6k1/8/8/8/1q6/2B5/1P6/4K3 b - - 0 1';
  // b2 should not be undefended
  const expected = ['c3:pin', 'b4:undefended', 'e1:checkable'].sort();
  assert.deepEqual(runAnalysis(fen), expected);
});

test('Undefended: Using more valuable attacker first', () => {
  const fen = '6k1/8/8/8/2r1BK2/8/1qP5/8 b - - 0 1';
  const expected = ['e4:pin', 'c2:undefended', 'f4:checkable'].sort();
  assert.deepEqual(runAnalysis(fen), expected);
});

test('Undefended: Not using attacker that does necessary pin', () => {
  const fen = '6k1/B7/1B3B2/2BrBNK1/3N2P1/2b5/1b1r1b2/b5q1 w - - 0 1';
  // d4 should be undefended, as the last piece Black can take with is the queen
  const expected = ['d4:undefended', 'g4:pin', 'g5:checkable', 'g8:checkable'].sort();
  assert.deepEqual(runAnalysis(fen), expected);
});

test('Undefended: Not using attacker that blocks multiple defenders', () => {
  const fen = '6k1/b7/1b6/2b2n2/3B2n1/4n3/4R3/2B1R1K1 w - - 0 1';
  // e3 should be undefended, since White can capture without using the d4-bishop
  // If we had an extremely contrived case like 6k1/b7/1b5b/2b3b1/3B1B2/4n3/4R3/6K1 w - - 0 1,
  // with multiple attackers each blocking multiple defenders, then the piece would wrongly be
  // labelled undefended (branching factor in `getSEE` is only 2).
  const expected = ['e3:undefended', 'd4:undefended'].sort();
  assert.deepEqual(runAnalysis(fen), expected);
});

test('Undefended: Not using a certain defender', () => {
  const fen = '6k1/b7/2n2B2/1n1rBNK1/3B4/1N6/2Nr1q2/B7 w - - 0 1';
  // d4 should not be undefended, since White can recapture with all but the f5-knight
  const expected = ['c2:undefended', 'd2:undefended', 'f2:undefended', 'g5:checkable', 'g8:checkable'].sort();
  assert.deepEqual(runAnalysis(fen), expected);
});

test('Crazyhouse', () => {
  const fen = 'rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R/ b KQkq - 1 2';
  const expected = ['e5:undefended'];
  assert.deepEqual(runAnalysis(fen), expected);
});

test('Three-Check', () => {
  const fen = 'rn2kbnr/pppbpppp/3p4/7Q/4P3/8/PPPP1PPP/RNB1K1NR b KQkq - 1 4 +2+0';
  const expected = ['f7:pin', 'h7:pin', 'e8:checkable'].sort();
  assert.deepEqual(runAnalysis(fen), expected);
});

test('Multiple tests (1)', () => {
  const fen = '8/6qk/1p2p3/pBn1p1pP/P3PbN1/2P2P2/KP1r2Q1/6R1 w - - 0 1';
  const expected = ['g2:undefended', 'b2:pin', 'a2:checkable', 'h7:checkable'].sort();
  assert.deepEqual(runAnalysis(fen), expected);
});

test('Multiple tests (2)', () => {
  const fen = '3r2k1/ppq2p2/2p1Nnpb/4p3/1B2P2P/P1N2PQ1/KPPn4/5B1R w - - 0 1';
  const expected = [
    'd8:undefended',
    'c7:undefended',
    'e6:undefended',
    'g6:pin',
    'e5:pin',
    'g8:checkable',
  ].sort();
  assert.deepEqual(runAnalysis(fen), expected);
});
