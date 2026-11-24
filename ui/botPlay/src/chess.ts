import type { Chess, Move as ChessMove } from 'chessops';

/* The currently displayed position, not necessarily the last one played */
export interface Board {
  onPly: Ply;
  chess: Chess;
  lastMove?: ChessMove;
}
