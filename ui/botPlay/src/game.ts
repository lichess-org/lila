import { Chess } from 'chessops';
import { Move as ChessMove } from 'chessops';
import { makeFen, parseFen } from 'chessops/fen';
import { defaultGame, parsePgn, type PgnNodeData, type Game as PgnGame } from 'chessops/pgn';
import { randomId } from 'lib/algo';
import { StatusName } from 'lib/game/game';
import type { ClockConfig, SetData as ClockState } from 'lib/game/clock/clockCtrl';
import { type BotId } from 'lib/bot/types';
import { DateMillis } from './interfaces';
import { Board } from './chess';
import { makeSan, parseSan } from 'chessops/san';
import { normalizeMove } from 'chessops/chess';

export interface Move {
  san: San;
  at: DateMillis;
}

interface GameEnd {
  winner?: Color;
  status: StatusName;
  fen: FEN;
}

export class Game {
  id: string;
  end?: GameEnd;

  constructor(
    readonly botId: BotId,
    readonly pov: Color,
    readonly clockConfig?: ClockConfig,
    readonly initialFen?: FEN,
    public moves: Move[] = [],
    id?: string,
  ) {
    this.id = id || 'b-' + randomId(10);
    this.recomputeEndFromLastBoard();
  }

  ply = (): Ply => this.moves.length;
  turn = (): Color => (this.ply() % 2 ? 'black' : 'white');

  isClockTicking = (): Color | undefined => (this.end || this.moves.length < 2 ? undefined : this.turn());

  computeClockState = (): ClockState | undefined => {
    const config = this.clockConfig;
    if (!config) return;
    const state = {
      white: config.initial,
      black: config.initial,
    };
    let lastMoveAt: DateMillis | undefined;
    this.moves.forEach(({ at }, i) => {
      const color = i % 2 ? 'black' : 'white';
      if (lastMoveAt && i > 1) {
        state[color] = Math.max(0, state[color] - (at - lastMoveAt) / 1000 + config.increment);
      }
      lastMoveAt = at;
    });
    const ticking = this.isClockTicking();
    if (ticking && lastMoveAt && this.moves.length > 1) state[ticking] -= (Date.now() - lastMoveAt) / 1000;
    return {
      ...state,
      ticking,
    };
  };

  rewindToPly = (ply: Ply): void => {
    this.moves = this.moves.slice(0, ply);
  };

  playMoveAtPly = (chessMove: ChessMove, ply: Ply): Move => {
    this.rewindToPly(ply);
    const chess = this.lastBoard().chess;
    const move: Move = {
      san: makeSan(chess, normalizeMove(chess, chessMove)),
      at: Date.now(),
    };
    this.moves.push(move);
    this.recomputeEndFromLastBoard();
    return move;
  };

  copyAtPly = (ply: Ply): Game => {
    if (ply >= this.ply()) return this;
    const moves = this.moves.slice(0, ply);
    return new Game(this.botId, this.pov, this.clockConfig, this.initialFen, moves);
  };

  toPgn = (): [PgnGame<PgnNodeData>, Chess] => {
    const headers = new Map<string, string>();
    if (this.initialFen) headers.set('FEN', this.initialFen);
    const pgn = parsePgn(this.moves.map(m => m.san).join(' '), () => headers)[0] || defaultGame();
    const chess: Chess = this.initialFen
      ? parseFen(this.initialFen)
          .chain(setup => Chess.fromSetup(setup))
          .unwrap(
            i => i,
            _ => Chess.default(),
          )
      : Chess.default();
    return [pgn, chess];
  };

  lastBoard = (): Board => {
    const [pgn, chess] = this.toPgn();
    const board: Board = { onPly: 0, chess };
    if (!pgn) return board;
    for (const node of pgn.moves.mainline()) {
      const move = parseSan(board.chess, node.san);
      if (!move) {
        // Illegal move
        console.warn('Illegal move', node.san);
        this.rewindToPly(board.onPly);
        break;
      }
      board.chess.play(move);
      board.onPly++;
      board.lastMove = move;
    }
    return board;
  };

  private recomputeEndFromLastBoard = (): void => {
    this.end = makeEndOf(this.lastBoard().chess);
  };
}

export const makeEndOf = (chess: Chess): GameEnd | undefined => {
  if (!chess.isEnd()) return;
  return {
    winner: chess.outcome()?.winner,
    status: chess.isCheckmate() ? 'mate' : chess.isStalemate() ? 'stalemate' : 'draw',
    fen: makeFen(chess.toSetup()),
  };
};
