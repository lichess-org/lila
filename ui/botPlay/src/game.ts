import { Chess, Move as ChessMove, opposite } from 'chessops';
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
import { computeClockState } from './clock';

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
    this.computeEnd();
  }

  ply = (): Ply => this.moves.length;
  turn = (): Color => (this.ply() % 2 ? 'black' : 'white');

  isClockTicking = (): Color | undefined => (this.end || this.moves.length < 2 ? undefined : this.turn());

  clockState = (): ClockState | undefined =>
    this.clockConfig && computeClockState(this.clockConfig, this.moves, this.isClockTicking());

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
    this.computeEnd();
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

  computeEnd = (): GameEnd | undefined => {
    const chess = this.lastBoard().chess;
    this.end = endOnTheBoard(chess);
    if (this.end) return this.end;
    const clock = this.clockState();
    if (!clock) return;
    const flag = (color: Color): GameEnd => ({
      winner: opposite(color),
      status: 'outoftime',
      fen: makeFen(chess.toSetup()),
    });
    if (clock?.white <= 0) this.end = flag('white');
    else if (clock?.black <= 0) this.end = flag('black');
    return this.end;
  };
}

const endOnTheBoard = (chess: Chess): GameEnd | undefined => {
  if (!chess.isEnd()) return;
  return {
    winner: chess.outcome()?.winner,
    status: chess.isCheckmate() ? 'mate' : chess.isStalemate() ? 'stalemate' : 'draw',
    fen: makeFen(chess.toSetup()),
  };
};
