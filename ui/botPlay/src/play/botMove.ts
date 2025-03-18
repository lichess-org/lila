import { Chess, makeUci, Move, parseUci } from 'chessops';
import type { MoveArgs, MoveSource } from 'local';
import type { Game } from '../game';
import { toPgn } from '../chess';
import { INITIAL_FEN } from 'chessops/fen';
import { parseSan } from 'chessops/san';
import { hashBoard } from 'chess/hash';

export const requestBotMove = async (source: MoveSource, game: Game): Promise<Move> => {
  const now = performance.now();

  const [ucis, hashes, chess] = makeUcisAndHashes(game);

  const threefoldMoves = makeThreefoldMoves(chess, hashes);

  const moveRequest: MoveArgs = {
    pos: { fen: INITIAL_FEN, moves: ucis },
    chess: Chess.default(),
    avoid: threefoldMoves,
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
  const uci = res && parseUci(res.uci);

  if (uci)
    return new Promise(resolve => {
      const waitTime = Math.max(0, res.movetime * 1000 - (performance.now() - now));
      setTimeout(() => resolve(uci), waitTime);
    });
  else return Promise.reject('no move');
};

const makeUcisAndHashes = (game: Game): [Uci[], bigint[], Chess] => {
  const pgn = toPgn(game);
  const chess = Chess.default();
  const ucis: Uci[] = [];
  const hashes: bigint[] = [];
  for (const node of pgn.moves.mainline()) {
    const move = parseSan(chess, node.san)!;
    chess.play(move);
    ucis.push(makeUci(move));
    if (chess.halfmoves == 0) hashes.length = 0;
    else hashes.push(hashBoard(chess.board));
  }
  return [ucis, hashes, chess];
};

const makeThreefoldMoves = (chess: Chess, hashes: bigint[]): Uci[] => {
  const tfms: Uci[] = [];
  for (const [from, dests] of chess.allDests()) {
    for (const to of dests) {
      const next = chess.clone();
      next.play({ from, to });
      const moveHash = hashBoard(next.board);
      if (hashes.filter(h => h == moveHash).length > 1) tfms.push(makeUci({ from, to }));
    }
  }
  return tfms;
};
