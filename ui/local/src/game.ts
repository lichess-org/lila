import * as co from 'chessops';
import { makeFen } from 'chessops/fen';
import { normalizeMove } from 'chessops/chess';
import { makeSanAndPlay } from 'chessops/san';
import { Outcome } from './types';
import { statusOf } from 'game/status';
import { Status } from 'game';

export interface GameState {
  startingFen: string;
  moves: Uci[];
  threefoldFens?: Map<string, number>;
  fiftyHalfMove?: number;
  white: string | undefined;
  black: string | undefined;
}

export class Game {
  moves: Uci[];
  chess: co.Chess;
  threefoldFens: Map<string, number> = new Map();
  fiftyHalfMove: number = 0;

  constructor(
    readonly startingFen: string = co.fen.INITIAL_FEN,
    moves: Uci[] = [],
  ) {
    this.moves = [];
    this.chess = co.Chess.fromSetup(co.fen.parseFen(this.startingFen).unwrap()).unwrap();
    this.threefoldFens = new Map();
    for (const move of moves ?? []) this.move(move);
  }

  move(uci: Uci): {
    end: boolean;
    san: San;
    move?: co.NormalMove;
    result?: Outcome | 'error';
    reason?: string;
    status?: Status;
  } {
    const bareMove = co.parseUci(uci) as co.NormalMove;
    const move = bareMove
      ? { ...(normalizeMove(this.chess, bareMove) as co.NormalMove), promotion: bareMove.promotion }
      : undefined;
    if (!move || !this.chess.isLegal(move)) {
      return {
        end: true,
        san: '',
        result: 'error',
        reason: `${this.turn} made illegal move ${uci} at ${makeFen(this.chess.toSetup())}`,
        status: statusOf('aborted'),
      };
    }
    uci = co.makeUci(move); // fix e1h1/e8h8 nonsense
    const san = makeSanAndPlay(this.chess, move);
    this.fifty(move);
    this.updateThreefold();
    const { end, result, reason, status } = this.checkGameOver();
    this.moves.push(uci);
    return { end, result, reason, status, san, move };
  }

  fifty(move?: co.NormalMove): boolean {
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
    const boardFen = this.fen.split('-')[0];
    let fenCount = this.threefoldFens.get(boardFen) ?? 0;
    this.threefoldFens.set(boardFen, ++fenCount);
    return fenCount >= 3; // TODO fixme
  }

  checkGameOver(userEnd?: 'whiteResign' | 'blackResign' | 'mutualDraw'): {
    end: boolean;
    result?: Outcome | 'error';
    reason?: string;
    status?: Status;
  } {
    let status = statusOf('started');
    let result: Outcome | 'error' = 'draw',
      reason = userEnd ?? 'checkmate';
    if (this.chess.isCheckmate()) [result, status] = [co.opposite(this.chess.turn), statusOf('mate')];
    else if (this.chess.isInsufficientMaterial()) [reason, status] = ['insufficient', statusOf('draw')];
    else if (this.chess.isStalemate()) [reason, status] = ['stalemate', statusOf('stalemate')];
    else if (this.fifty()) [reason, status] = ['fifty', statusOf('draw')];
    else if (this.isThreefold) [reason, status] = ['threefold', statusOf('draw')];
    else if (userEnd) {
      if (userEnd !== 'mutualDraw') [reason, status] = ['resign', statusOf('resign')];
      if (userEnd === 'whiteResign') result = 'black';
      else if (userEnd === 'blackResign') result = 'white';
    } else return { end: false, status };
    // needs outoftime
    return { end: true, result, reason, status };
  }

  get ply(): number {
    return 2 * (this.chess.fullmoves - 1) + (this.chess.turn === 'black' ? 1 : 0);
  }

  get turn(): Color {
    return this.chess.turn;
  }

  get isThreefold(): boolean {
    return (this.threefoldFens.get(this.fen.split('-')[0]) ?? 0) >= 3; // TODO fixme
  }

  get fen(): string {
    return makeFen(this.chess.toSetup());
  }

  get dests(): { [from: string]: string } {
    const dests: { [from: string]: string } = {};
    [...this.chess.allDests()]
      .filter(([, to]) => !to.isEmpty())
      .forEach(([s, ds]) => (dests[co.makeSquare(s)] = [...ds].map(co.makeSquare).join('')));
    return dests;
  }

  get cgDests(): Map<Cg.Key, Cg.Key[]> {
    const dec = new Map();
    const dests = this.dests;
    if (!dests) return dec;
    for (const k in dests) dec.set(k, dests[k].match(/.{2}/g) as Cg.Key[]);
    return dec;
  }
}
