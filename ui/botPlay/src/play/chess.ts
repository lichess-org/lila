import { Chess, Move } from 'chessops';
import { parsePgn } from 'chessops/pgn';
import { Game } from '../interfaces';
import { makeSanAndPlay, parseSan } from 'chessops/san';
import { normalizeMove } from 'chessops/chess';
import { defined } from 'common';
import { StatusName } from 'game';
import { makeFen } from 'chessops/fen';

/* The currently displayed position, not necessarily the last one played */
export interface Board {
  onPly: Ply;
  chess: Chess;
  lastMove?: Move;
  end?: GameEnd;
}
interface GameEnd {
  winner?: Color;
  status: StatusName;
  fen: FEN;
}

export const makeBoardAt = (game: Game, onPly: Ply): Board => {
  const pgn = toPgn(game, onPly);
  const board: Board = { onPly: 0, chess: Chess.default() };
  if (!pgn) return board;
  for (const node of pgn.moves.mainline()) {
    const move = parseSan(board.chess, node.san);
    if (!move) {
      // Illegal move
      console.warn('Illegal move', node.san);
      game.sans = game.sans.slice(0, board.onPly);
      break;
    }
    board.chess.play(move);
    board.onPly++;
    board.lastMove = move;
  }
  board.end = endOf(board.chess);
  return board;
};

export const addMove = (board: Board, move: Move): San => {
  const san = makeSanAndPlay(board.chess, normalizeMove(board.chess, move));
  board.onPly++;
  board.lastMove = move;
  board.end = endOf(board.chess);
  return san;
};

export const toPgn = (game: Game, plies?: Ply) =>
  parsePgn((defined(plies) ? game.sans.slice(0, plies) : game.sans).join(' '))[0];

const endOf = (chess: Chess): GameEnd | undefined => {
  if (!chess.isEnd()) return;
  return {
    winner: chess.outcome()?.winner,
    status: chess.isCheckmate() ? 'mate' : chess.isStalemate() ? 'stalemate' : 'draw',
    fen: makeFen(chess.toSetup()),
  };
};
