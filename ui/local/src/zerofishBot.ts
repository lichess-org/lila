import * as co from 'chessops';
import { zip, clamp } from 'common';
import { normalize, interpolate } from './operator';
import { outcomeExpectancy, getNormal } from './util';
import { zerofishMove, zerofishThink } from './zerofishMove';
import type { FishSearch, Position, Zerofish } from 'zerofish';
import type { OpeningBook } from 'bits/polyglot';
import type { AssetDb } from './assetDb';
import type {
  BotInfo,
  Libot,
  ZerofishBotInfo,
  ZeroSearch,
  Operator,
  Glicko,
  Book,
  MoveArgs,
  MoveResult,
} from './types';

export type ZerofishBots = { [id: string]: ZerofishBot };

export class ZerofishBot implements Libot, ZerofishBotInfo {
  private zerofish: Zerofish;
  private assetDb: AssetDb;
  private openings: Promise<OpeningBook[]>;
  private stats: { cplMoves: number; cpl: number };
  readonly uid: string;
  name: string;
  description: string;
  image: string;
  version: number = 0;
  books: Book[] = [];
  zero?: ZeroSearch;
  fish?: FishSearch;
  glicko?: Glicko;
  operators?: { [type: string]: Operator };

  constructor(info: BotInfo, zerofish: Zerofish, assetDb: AssetDb) {
    Object.assign(this, structuredClone(info));
    Object.values(this.operators ?? {}).forEach(normalize);

    // non enumerable properties are not stored or cloned
    Object.defineProperty(this, 'zerofish', { value: zerofish });
    Object.defineProperty(this, 'assetDb', { value: assetDb });
    Object.defineProperty(this, 'openings', {
      get: () => Promise.all(this.books ? [...this.books.map(b => this.assetDb.getBook(b.name))] : []),
    });
    Object.defineProperty(this, 'stats', { value: { cplMoves: 0, cpl: 0 } });
  }

  get ratingText(): string {
    return `${this.glicko?.r ?? 1500}${(this.glicko?.rd ?? 3580) > 80 ? '?' : ''}`;
  }

  get fullRatingText(): string {
    return this.ratingText + ` (${Math.round(this.glicko?.rd ?? 350)})`;
  }

  get statsText(): string {
    return this.stats.cplMoves ? `acpl ${Math.round(this.stats.cpl / this.stats.cplMoves)}` : '';
  }

  async move(moveArgs: MoveArgs): Promise<MoveResult> {
    const { pos, chess } = moveArgs;
    const opening = await this.bookMove(chess);
    if (opening) return { uci: opening, time: zerofishThink(this, moveArgs) };
    const [zeroResult, fishResult] = await Promise.all([
      this.zero &&
        this.zerofish.goZero(pos, {
          ...this.zero,
          net: {
            name: this.name + '-' + this.zero.net,
            fetch: async () => (await this.assetDb.getNet(this.zero!.net))!,
          },
        }),
      this.fish && this.zerofish.goFish(pos, this.fish),
    ]);
    const { move, cpl, time } = zerofishMove(fishResult, zeroResult, this, moveArgs);
    if (cpl !== undefined && cpl < 1000) {
      this.stats.cplMoves++; // debug stats
      this.stats.cpl += cpl;
    }
    return { uci: move, time };
  }

  operator(op: string, { chess, score, secondsRemaining }: MoveArgs): undefined | number {
    const o = this.operators?.[op];
    if (!o) return undefined;
    return interpolate(
      o,
      o.from === 'move'
        ? chess.fullmoves
        : o.from === 'score'
        ? outcomeExpectancy(chess.turn, score ?? 0)
        : secondsRemaining ?? Infinity,
    );
  }

  private async bookMove(chess: co.Chess) {
    if (!this.books?.length) return undefined;
    const moveList: { moves: { uci: Uci; weight: number }[]; bookWeight: number }[] = [];
    let bookChance = 0;
    for (const [book, opening] of zip(this.books, await this.openings)) {
      const moves = opening(chess);
      if (moves.length === 0) continue;
      moveList.push({ moves, bookWeight: book.weight ?? 1 });
      bookChance += book.weight ?? 1;
    }
    bookChance = Math.random() * bookChance;
    for (const { moves, bookWeight } of moveList) {
      bookChance -= bookWeight;
      if (bookChance <= 0) {
        let chance = Math.random();
        for (const { uci, weight } of moves) {
          chance -= weight;
          if (chance <= 0) return uci;
        }
      }
    }
    return undefined;
  }
}
