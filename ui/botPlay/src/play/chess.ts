import { Chess, Move } from 'chessops';
import { parsePgn } from 'chessops/pgn';
import { Game } from '../interfaces';
import { makeSanAndPlay, parseSan } from 'chessops/san';
import { normalizeMove } from 'chessops/chess';

export interface Board {
  onPly: Ply;
  chess: Chess;
  lastMove?: Move;
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
  return board;
};

export const addMove = (board: Board, game: Game, move: Move) => {
  const san = makeSanAndPlay(board.chess, normalizeMove(board.chess, move));
  game.sans = game.sans.slice(0, board.onPly);
  game.sans.push(san);
  board.onPly = game.sans.length;
  board.lastMove = move;
};
