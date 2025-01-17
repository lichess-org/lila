import * as co from 'chessops';
import { normalizeMove } from 'chessops/chess';
import { statusOf } from './status';
import type { Status, LocalSetup, RoundStep } from './interfaces';
import { deepFreeze, randomToken } from 'common/algo';
import { hashBoard } from 'chess/hash';

export interface GameStatus {
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

type LocalMove = {
  uci: Uci;
  clock?: { white: Seconds; black: Seconds };
};

export class LocalGame implements LocalSetup {
  id: string;
  moves: LocalMove[] = [];
  chess: co.Chess;
  initialPly: number = 0;
  initialFen: FEN;
  initial: Seconds = Infinity;
  white?: string;
  black?: string;
  increment: Seconds = 0;
  threefoldHashes: Map<bigint, number> = new Map();
  fiftyHalfMove: number = 0;
  finished?: GameStatus;

  constructor(o: { game: LocalGame; ply?: number } | { setup: LocalSetup }) {
    Object.assign(this, 'game' in o ? o.game : o.setup);

    this.id ??= randomToken();
    this.initialFen ??= co.fen.INITIAL_FEN;
    this.chess = co.Chess.fromSetup(co.fen.parseFen(this.initialFen).unwrap()).unwrap();
    this.initialPly = 2 * (this.chess.fullmoves - 1) + (this.chess.turn === 'black' ? 1 : 0);
    if ('game' in o && o.game) {
      this.moves = [];
      this.threefoldHashes = new Map();
      this.fiftyHalfMove = 0;
      for (const move of o.game.moves.slice(0, o.ply)) this.move(move);
    }
  }

  move(move: LocalMove): MoveContext {
    //if (this.end) return this.processResult(move);
    const { move: coMove, uci } = normalMove(this.chess, move.uci) ?? {};
    if (!coMove || !uci) {
      return this.processResult({
        uci: move.uci,
        reason: `${this.turn} made illegal move ${move.uci} at ${this.fen}`,
        status: statusOf('unknownFinish'),
      });
    }
    const san = co.san.makeSanAndPlay(this.chess, coMove);
    const clock = move.clock ? { white: move.clock.white, black: move.clock.black } : undefined;
    this.moves.push({ uci, clock });
    this.fifty(coMove);
    this.updateThreefold();
    return this.processResult({ uci, san, move: coMove });
  }

  finish(finishStatus: Omit<GameStatus, 'end' | 'turn'>): void {
    if (Object.isFrozen(this)) return;
    this.finished = { ...this.status, ...finishStatus };
    deepFreeze(this);
  }

  get clock(): { white: number; black: number } | undefined {
    return this.moves.length
      ? this.moves[this.moves.length - 1].clock
      : Number.isFinite(this.initial)
        ? { white: this.initial, black: this.initial }
        : undefined;
  }

  get status(): GameStatus {
    return (
      this.finished ?? {
        winner: this.chess.outcome()?.winner,
        turn: this.chess.turn,
        ...(this.chess.isCheckmate()
          ? { status: statusOf('mate') }
          : this.chess.isInsufficientMaterial()
            ? { reason: 'insufficient', status: statusOf('draw') }
            : this.chess.isStalemate()
              ? { status: statusOf('stalemate') }
              : this.fifty()
                ? { reason: 'fifty', status: statusOf('draw') }
                : this.isThreefold
                  ? { reason: 'threefold', status: statusOf('draw') }
                  : { status: statusOf(this.ply > 0 ? 'started' : 'created') }),
      }
    );
  }

  get ply(): number {
    return this.moves.length;
  }

  get turn(): Color {
    return this.chess.turn;
  }

  get awaiting(): Color {
    return co.opposite(this.chess.turn);
  }

  get isThreefold(): boolean {
    return (this.threefoldHashes.get(hashBoard(this.chess.board)) ?? 0) > 2;
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

  get steps(): RoundStep[] {
    const steps: RoundStep[] = [{ fen: this.initialFen, ply: 0, uci: '', san: '', check: false }];
    const chess = co.Chess.fromSetup(co.fen.parseFen(this.initialFen).unwrap()).unwrap();
    for (const move of this.moves) {
      const { move: coMove } = normalMove(chess, move.uci) ?? {};
      if (!coMove) break;
      const san = co.san.makeSanAndPlay(chess, coMove);
      steps.push({
        uci: move.uci,
        san,
        fen: co.fen.makeFen(chess.toSetup()),
        check: chess.isCheck(),
        ply: steps.length,
      });
    }
    return steps;
  }

  get threefoldDraws(): Uci[] {
    const draws: Uci[] = [];
    const boardHash = hashBoard(this.chess.board);
    for (const [from, dests] of this.chess.allDests()) {
      for (const to of dests) {
        const chess = this.chess.clone();
        chess.play({ from, to });
        const moveHash = hashBoard(chess.board);
        if (moveHash !== boardHash && (this.threefoldHashes.get(moveHash) ?? 0) > 1)
          draws.push(co.makeUci({ from, to }));
      }
    }
    return draws;
  }

  get setup(): LocalSetup {
    return {
      initialFen: this.initialFen,
      initial: this.initial,
      increment: this.increment,
      white: this.white,
      black: this.black,
    };
  }

  private processResult(fields: Partial<MoveContext>): MoveContext {
    const result = {
      uci: '',
      san: '',
      fen: this.fen,
      ply: this.ply,
      justPlayed: this.awaiting,
      dests: this.dests,
      threefold: this.isThreefold,
      check: this.chess.isCheck(),
      fifty: this.fifty(),
      ...this.status,
      ...fields,
    };
    if (!['started', 'created'].includes(result.status.name)) this.finish(result);
    return result;
  }

  private fifty(move?: co.NormalMove): boolean {
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

  private updateThreefold(): boolean {
    const boardHash = hashBoard(this.chess.board);
    const count = (this.threefoldHashes.get(boardHash) ?? 0) + 1;
    this.threefoldHashes.set(boardHash, count);
    return count > 2;
  }
}

export function normalMove(chess: co.Chess, uci: Uci): { uci: Uci; move: co.NormalMove } | undefined {
  const bareMove = co.parseUci(uci);
  const move =
    bareMove && 'from' in bareMove ? { ...bareMove, ...normalizeMove(chess, bareMove) } : undefined;
  return move && chess.isLegal(move) ? { uci: co.makeUci(move), move } : undefined;
}
