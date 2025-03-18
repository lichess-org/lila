import { Chess, Move } from 'chessops';
import { defaultGame, parsePgn, type PgnNodeData, type Game as PgnGame } from 'chessops/pgn';
import { Game } from './game';
import { makeSanAndPlay, parseSan } from 'chessops/san';
import { normalizeMove } from 'chessops/chess';
import { defined } from 'common';
import { parseFen } from 'chessops/fen';

/* The currently displayed position, not necessarily the last one played */
export interface Board {
  onPly: Ply;
  chess: Chess;
  lastMove?: Move;
}

export const makeBoardAt = (game: Game, onPly?: Ply): Board => {
  const [pgn, chess] = toPgn(game, onPly ?? game.sans.length);
  const board: Board = { onPly: 0, chess };
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
  return board;
};

export const addMove = (board: Board, move: Move): San => {
  const san = makeSanAndPlay(board.chess, normalizeMove(board.chess, move));
  board.onPly++;
  board.lastMove = move;
  return san;
};

export function toPgn(game: Game, plies?: Ply): [PgnGame<PgnNodeData>, Chess] {
  const headers = new Map<string, string>();
  if (game.initialFen) headers.set('FEN', game.initialFen);
  const sans = defined(plies) ? game.sans.slice(0, plies) : game.sans;
  const pgn = parsePgn(sans.join(' '), () => headers)[0] || defaultGame();
  const chess: Chess = game.initialFen
    ? parseFen(game.initialFen)
        .chain(setup => Chess.fromSetup(setup))
        .unwrap(
          i => i,
          _ => Chess.default(),
        )
    : Chess.default();
  return [pgn, chess];
}
