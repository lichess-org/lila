import { Chess, makeUci, Move, parseUci } from 'chessops';
import type { MoveArgs, MoveSource } from 'local';
import type { Game } from '../interfaces';
import { toPgn } from './chess';
import { INITIAL_FEN } from 'chessops/fen';
import { parseSan } from 'chessops/san';

export const requestBotMove = async (source: MoveSource, game: Game): Promise<Move> => {
  const pgn = toPgn(game);
  const chess = Chess.default();
  const ucis = Array.from(pgn.moves.mainline()).map(node => {
    const move = parseSan(chess, node.san)!;
    chess.play(move);
    return makeUci(move);
  });

  const moveRequest: MoveArgs = {
    pos: { fen: INITIAL_FEN, moves: ucis },
    chess: chess,
    avoid: [], // threefold moves
    initial: Infinity,
    remaining: Infinity,
    opponentRemaining: Infinity,
    increment: 0,
    // initial: this.clock?.initial ?? Infinity,
    // remaining: this.clock?.[game.turn] ?? Infinity,
    // opponentRemaining: this.clock?.[game.awaiting] ?? Infinity,
    // increment: this.clock?.increment ?? 0,
    ply: game.sans.length,
  };

  const res = await source.move(moveRequest);

  return parseUci(res!.uci)!;
};
