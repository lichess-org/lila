import assert from 'node:assert/strict';
import { describe, test } from 'node:test';

import { endgameShapesForNode } from '../src/game/endgame';

const START = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';
const MATE = 'rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3';
const STALEMATE = '7k/5Q2/6K1/8/8/8/8/8 b - - 0 1';

// a node the way AnalyseCtrl hands one to the shape builder
const node = (fen: FEN, opts: { terminal?: boolean; check?: boolean; winner?: Color } = {}) => ({
  fen,
  outcome: () => (opts.terminal ? { winner: opts.winner } : undefined),
  check: () => !!opts.check,
  dests: () => (opts.terminal ? new Map() : new Map([['e2', ['e3', 'e4']]])) as Dests,
});

describe('endgame shapes', () => {
  test('a position that is not an ending draws nothing without a game result', () =>
    assert.deepEqual(endgameShapesForNode(node(START), undefined, undefined), []));

  test('the game result is drawn when it is supplied', () =>
    assert.equal(endgameShapesForNode(node(START), 'white', 'resign').length, 2));

  test('checkmate is drawn from the position alone', () =>
    assert.equal(
      endgameShapesForNode(node(MATE, { terminal: true, check: true, winner: 'black' }), undefined, undefined)
        .length,
      2,
    ));

  test('stalemate is drawn from the position alone', () =>
    assert.equal(endgameShapesForNode(node(STALEMATE, { terminal: true }), undefined, undefined).length, 2));

  test('an aborted game draws nothing', () =>
    assert.deepEqual(endgameShapesForNode(node(START), undefined, 'aborted'), []));
});
