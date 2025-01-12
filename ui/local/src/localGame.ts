import * as co from 'chessops';
import { normalizeMove } from 'chessops/chess';
import { statusOf } from 'game/status';
import { Status } from 'game';
import { deepFreeze } from 'common/algo';
import { env } from './localEnv';

export interface GameStatus {
  end: boolean; // convenience, basically anything after status 'started'
  status: Status;
  turn: Color;
  winner?: Color;
  reason?: string;
}

export interface MoveContext extends GameStatus {
  justPlayed: Color;
  uci: Uci;
  san: San;
  move?: co.NormalMove;
  fen: string;
  ply: number;
  dests: { [from: string]: string };
  threefold: boolean;
  check: boolean;
  fifty: boolean;
  winner?: Color;
  silent?: boolean;
}

type LocalMove = { uci: Uci; clock?: { white: number; black: number } };

export class LocalGame {
  moves: LocalMove[];
  chess: co.Chess;
  initialPly: number = 0;
  threefoldHashes: Map<bigint, number> = new Map();
  fiftyHalfMove: number = 0;
  finished?: GameStatus;

  constructor(
    readonly initialFen: string = co.fen.INITIAL_FEN,
    moves: LocalMove[] = [],
  ) {
    this.chess = co.Chess.fromSetup(co.fen.parseFen(this.initialFen).unwrap()).unwrap();
    this.threefoldHashes = new Map();
    this.moves = [];
    for (const move of moves ?? []) this.move(move);
    this.initialPly = 2 * (this.chess.fullmoves - 1) + (this.chess.turn === 'black' ? 1 : 0);
  }

  move(move: LocalMove): MoveContext {
    if (this.end) return this.moveResultWith(move);
    const { move: coMove, uci } = normalMove(this.chess, move.uci) ?? {};
    if (!coMove || !uci) {
      return this.moveResultWith({
        end: true,
        uci: move.uci,
        reason: `${this.turn} made illegal move ${move.uci} at ${co.fen.makeFen(this.chess.toSetup())}`,
        status: statusOf('cheat'), // bots are sneaky
      });
    }
    const san = co.san.makeSanAndPlay(this.chess, coMove);
    const clock = move.clock ? { white: move.clock.white, black: move.clock.black } : undefined;
    this.moves.push({ uci, clock });
    this.fifty(coMove);
    this.updateThreefold();
    return this.moveResultWith({ uci, san, move: coMove });
  }

  fifty(move?: co.NormalMove): boolean {
    // TODO: replace with chessops
    if (move) {
      if (this.chess.board.getRole(move.from) === 'pawn' || this.chess.board.get(move.to)) {
        this.fiftyHalfMove = 0;
      } else {
        this.fiftyHalfMove++;
      }
    }
    return this.fiftyHalfMove >= 100;
  }

  updateThreefold(): boolean {
    const boardHash = env.hash(this.chess.board);
    let count = this.threefoldHashes.get(boardHash) ?? 0;
    this.threefoldHashes.set(boardHash, count + 1);
    return count > 1;
  }

  finish(finishStatus: Omit<GameStatus, 'end' | 'turn'>): void {
    this.finished = { ...this.status, ...finishStatus, end: true };
    deepFreeze(this);
  }

  moveResultWith(fields: Partial<MoveContext>): MoveContext {
    return {
      uci: '',
      san: '',
      fen: this.fen,
      ply: this.ply,
      justPlayed: co.opposite(this.turn),
      dests: this.dests,
      threefold: this.isThreefold,
      check: this.chess.isCheck(),
      fifty: this.fifty(),
      ...this.status,
      ...fields,
    };
  }

  get clock(): { white: number; black: number } | undefined {
    return this.moves.length
      ? this.moves[this.moves.length - 1].clock
      : Number.isFinite(env.game.initial)
        ? { white: env.game.initial, black: env.game.initial }
        : undefined;
  }

  get status(): GameStatus {
    const gameStatus: { status: Status; reason?: string; winner?: Color } = this.chess.isCheckmate()
      ? { status: statusOf('mate') }
      : this.chess.isInsufficientMaterial()
        ? { reason: 'insufficient', status: statusOf('draw') }
        : this.chess.isStalemate()
          ? { status: statusOf('stalemate') }
          : this.fifty()
            ? { reason: 'fifty', status: statusOf('draw') }
            : this.isThreefold
              ? { reason: 'threefold', status: statusOf('draw') }
              : { status: statusOf(this.ply > 0 ? 'started' : 'created') };

    return {
      end: this.end,
      winner: this.chess.outcome()?.winner,
      turn: this.chess.turn,
      ...gameStatus,
      ...this.finished,
    };
  }

  get end(): boolean {
    return Object.isFrozen(this) || this.chess.isEnd() || this.fifty() || this.isThreefold;
  }

  get ply(): number {
    return 2 * (this.chess.fullmoves - 1) + (this.chess.turn === 'black' ? 1 : 0) - this.initialPly;
  }

  get turn(): Color {
    return this.chess.turn;
  }

  get isThreefold(): boolean {
    return (this.threefoldHashes.get(env.hash(this.chess.board)) ?? 0) > 2;
  }

  get fen(): string {
    return co.fen.makeFen(this.chess.toSetup());
  }

  get dests(): { [from: string]: string } {
    return Object.fromEntries([...this.cgDests].map(([src, dests]) => [src, dests.join('')]));
  }

  get cgDests(): Map<Key, Key[]> {
    return co.compat.chessgroundDests(this.chess);
  }

  get threefoldDraws(): Uci[] {
    const draws: Uci[] = [];
    const boardHash = env.hash(this.chess.board);
    for (const [from, dests] of this.chess.allDests()) {
      for (const to of dests) {
        const chess = this.chess.clone();
        chess.play({ from, to });
        const moveHash = env.hash(chess.board);
        if (moveHash !== boardHash && (this.threefoldHashes.get(moveHash) ?? 0) > 1)
          draws.push(co.makeUci({ from, to }));
      }
    }
    return draws;
  }
}

export function normalMove(chess: co.Chess, uci: Uci): { uci: Uci; move: co.NormalMove } | undefined {
  const bareMove = co.parseUci(uci);
  const move =
    bareMove && 'from' in bareMove ? { ...bareMove, ...normalizeMove(chess, bareMove) } : undefined;
  return move && chess.isLegal(move) ? { uci: co.makeUci(move), move } : undefined;
}
