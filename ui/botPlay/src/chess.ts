import { Chess, type Move as ChessMove } from 'chessops';
import { normalizeMove } from 'chessops/chess';
import { makeSanAndPlay } from 'chessops/san';

/* The currently displayed position, not necessarily the last one played */
export interface Board {
  onPly: Ply;
  chess: Chess;
  lastMove?: ChessMove;
}

export const addMove = (board: Board, move: ChessMove): San => {
  const san = makeSanAndPlay(board.chess, normalizeMove(board.chess, move));
  board.onPly++;
  board.lastMove = move;
  return san;
};
