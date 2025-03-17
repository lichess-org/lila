import { Chess, Move } from 'chessops';
import { parsePgn } from 'chessops/pgn';
import { Game } from '../interfaces';
import { makeSanAndPlay, parseSan } from 'chessops/san';
import { normalizeMove } from 'chessops/chess';

/* The currently displayed position, not necessarily the last one played */
export interface Board {
  onPly: Ply;
  chess: Chess;
  lastMove?: Move;
  isEnd?: boolean;
}

export const makeBoardAt = (game: Game, onPly: Ply): Board => {
  const pgn = parsePgn(game.sans.slice(0, onPly).join(' '))[0];
  const board: Board = { onPly: 0, chess: Chess.default() };
  if (!pgn) return board;
  for (const node of pgn.moves.mainline()) {
    const move = parseSan(board.chess, node.san);
    if (!move) break; // Illegal move
    board.chess.play(move);
    board.onPly++;
    board.lastMove = move;
  }
  board.isEnd = board.chess.isEnd();
  return board;
};

export const addMove = (board: Board, move: Move): San => {
  const san = makeSanAndPlay(board.chess, normalizeMove(board.chess, move));
  board.onPly++;
  board.lastMove = move;
  board.isEnd = board.chess.isEnd();
  return san;
};
