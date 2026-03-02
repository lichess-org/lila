import * as co from 'chessops';
import { normalMove } from 'lib/game/chess';
import { type Status, type RoundStep, statusOf } from 'lib/game';
import { deepFreeze, randomId } from 'lib/algo';
import { hashBoard } from 'lib/game/hash';
import type { LocalSetup } from 'lib/bot/types';

type LocalMove = {
  uci: Uci;
  clock?: { white: Seconds; black: Seconds };
};

export interface GameStatus {
  status: Status;
  winner?: Color;
  reason?: string;
}

export interface GameContext extends GameStatus {
  uci: Uci;
  san: San;
  turn: Color;
  move?: co.NormalMove;
  fen: string;
  ply: number;
  dests: Record<string, string>;
  threefold: boolean;
  check: boolean;
  fiftyMoves: boolean;
  silent?: boolean;
  clock?: { white: number; black: number };
}

export class LocalGameData implements LocalSetup {
  id: string;
  createdAt: number;
  moves: LocalMove[];
  setupFen?: FEN;
  initial: Seconds;
  increment: Seconds;
  white?: string;
  black?: string;
  finished?: GameStatus;
}

export class LocalGame extends LocalGameData {
  private threefoldHashes: Map<bigint, number>;
  readonly chess: co.Chess;
  readonly initialPly: number;

  constructor(data?: LocalSetup | LocalGameData, ply?: number) {
    super();
    Object.assign(this, data);
    const chess = this.setupFen
      ? co.Chess.fromSetup(co.fen.parseFen(this.setupFen).unwrap()).unwrap()
      : co.Chess.default();
    Object.defineProperties(this, {
      threefoldHashes: { value: new Map() },
      chess: { value: chess },
      initialPly: { value: 2 * (chess.fullmoves - 1) + (chess.turn === 'black' ? 1 : 0) },
    });
    this.id ??= randomId();
    this.createdAt ??= Date.now();
    this.initial ??= Infinity;
    this.increment ??= 0;
    this.moves = [];
    this.finished = undefined;
    const initialMoves = data && 'moves' in data ? data.moves : [];
    for (const move of initialMoves.slice(0, ply)) {
      this.move(move);
    }
  }

  move(move: LocalMove): GameContext {
    const { move: coMove, uci } = normalMove(this.chess, move.uci) ?? {};
    if (!coMove || !uci) {
      return this.moveResult({
        uci: move.uci,
        reason: `${this.turn} made illegal move ${move.uci} at ${this.fen}`,
        status: statusOf('unknownFinish'),
      });
    }
    const san = co.san.makeSanAndPlay(this.chess, coMove);
    const boardHash = hashBoard(this.chess.board);
    const clock = structuredClone(move.clock);

    this.threefoldHashes.set(boardHash, (this.threefoldHashes.get(boardHash) ?? 0) + 1);
    this.moves.push({ uci, clock });

    return this.moveResult({ uci, san, move: coMove, clock });
  }

  finish(finishStatus: Omit<GameStatus, 'end' | 'turn'>): void {
    if (this.finished) return;
    this.finished = { ...this.status, ...finishStatus };
    deepFreeze(this);
  }

  *observe(): Generator<GameContext> {
    const chess = new LocalGame(this, 0);
    for (const move of this.moves) {
      yield chess.move(move);
    }
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
        ...(this.chess.isCheckmate()
          ? { status: statusOf('mate') }
          : this.chess.isInsufficientMaterial()
            ? { reason: 'insufficient', status: statusOf('draw') }
            : this.chess.isStalemate()
              ? { status: statusOf('stalemate') }
              : this.chess.halfmoves > 99
                ? { reason: 'fifty', status: statusOf('draw') }
                : this.isThreefold
                  ? { reason: 'threefold', status: statusOf('draw') }
                  : { status: statusOf(this.ply > 0 ? 'started' : 'created') }),
      }
    );
  }

  get ply(): number {
    return this.initialPly + this.moves.length;
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

  get initialFen(): string {
    return this.setupFen ?? co.fen.INITIAL_FEN;
  }

  get dests(): Record<string, string> {
    return Object.fromEntries([...this.cgDests].map(([src, dests]) => [src, dests.join('')]));
  }

  get cgDests(): Map<Key, Key[]> {
    return co.compat.chessgroundDests(this.chess);
  }

  get roundSteps(): RoundStep[] {
    const chess = this.setupFen
      ? co.Chess.fromSetup(co.fen.parseFen(this.setupFen).unwrap()).unwrap()
      : co.Chess.default();
    const steps: RoundStep[] = [
      {
        fen: this.setupFen ?? co.fen.INITIAL_FEN,
        ply: this.initialPly,
        uci: '',
        san: '',
        check: chess.isCheck(),
      },
    ];
    for (const move of this.moves) {
      const { move: coMove } = normalMove(chess, move.uci) ?? {};
      if (!coMove) break;
      const san = co.san.makeSanAndPlay(chess, coMove);
      steps.push({
        uci: move.uci,
        san,
        fen: co.fen.makeFen(chess.toSetup()),
        check: chess.isCheck(),
        ply: steps.length + this.initialPly,
      });
    }
    return steps;
  }

  get threefoldMoves(): Uci[] {
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
      setupFen: this.setupFen,
      initial: this.initial,
      increment: this.increment,
      white: this.white,
      black: this.black,
    };
  }

  private moveResult(fields: Partial<GameContext>): GameContext {
    const result = {
      uci: '',
      san: '',
      turn: this.chess.turn,
      fen: this.fen,
      ply: this.ply,
      dests: this.dests,
      threefold: this.isThreefold,
      check: this.chess.isCheck(),
      fiftyMoves: this.chess.halfmoves > 99,
      ...this.status,
      ...fields,
    };
    if (!['started', 'created'].includes(result.status.name)) this.finish(result);
    return result;
  }
}
