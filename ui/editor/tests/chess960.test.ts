import { parseSquare } from 'chessops';
import { parseFen } from 'chessops/fen';
import assert from 'node:assert/strict';
import { describe, test } from 'node:test';

import { castlingRooksFromBoard } from '../src/chess960';

const boardOf = (boardFen: string) => parseFen(`${boardFen} w - - 0 1`).unwrap().board;

describe('castling rooks from board', () => {
  test('standard back rank uses the corner rooks', () => {
    const board = boardOf('rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR');

    assert.deepEqual(castlingRooksFromBoard(board, 'white'), {
      rookQ: parseSquare('a1'),
      rookK: parseSquare('h1'),
    });
    assert.deepEqual(castlingRooksFromBoard(board, 'black'), {
      rookQ: parseSquare('a8'),
      rookK: parseSquare('h8'),
    });
  });

  test('a rook off the corner is still a castling rook', () => {
    const board = boardOf('rnbqk1r1/pppppppp/8/8/8/8/PPPPPPPP/RNBQK1R1');

    assert.deepEqual(castlingRooksFromBoard(board, 'white'), {
      rookQ: parseSquare('a1'),
      rookK: parseSquare('g1'),
    });
    assert.deepEqual(castlingRooksFromBoard(board, 'black'), {
      rookQ: parseSquare('a8'),
      rookK: parseSquare('g8'),
    });
  });

  test('the outermost rook on each side is picked', () => {
    const board = boardOf('8/8/8/8/8/8/8/RR2K1RR');

    assert.deepEqual(castlingRooksFromBoard(board, 'white'), {
      rookQ: parseSquare('a1'),
      rookK: parseSquare('h1'),
    });
  });

  test('rooks on one side only grant that side', () => {
    const board = boardOf('8/8/8/8/8/8/8/RR2K3');

    assert.deepEqual(castlingRooksFromBoard(board, 'white'), {
      rookQ: parseSquare('a1'),
      rookK: undefined,
    });
  });

  test('a king off the back rank has no castling rooks', () => {
    const board = boardOf('8/8/8/8/8/8/4K3/R6R');

    assert.deepEqual(castlingRooksFromBoard(board, 'white'), {});
  });

  test('rooks off the back rank are ignored', () => {
    const board = boardOf('8/8/8/8/8/8/R6R/4K3');

    assert.deepEqual(castlingRooksFromBoard(board, 'white'), {
      rookQ: undefined,
      rookK: undefined,
    });
  });
});
