import * as co from 'chessops';
import { zip } from 'common';
import { normalize } from './operator';
import { zerofishMove } from './zerofishMove';
import type { FishSearch, Position, Zerofish } from 'zerofish';
import type { OpeningBook } from 'bits/polyglot';
import type { AssetDb } from './assetDb';
import type { BotInfo, Libot, ZerofishBotInfo, ZeroSearch, Operator, Glicko, Book } from './types';

export type ZerofishBots = { [id: string]: ZerofishBot };

export class ZerofishBot implements Libot, ZerofishBotInfo {
  private zerofish: Zerofish;
  private assetDb: AssetDb;
  private openings: Promise<OpeningBook[]>;
  private stats: any;
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
    Object.assign(this, info);
    Object.values(this.operators ?? {}).forEach(normalize);

    // non enumerable properties are not stored or cloned
    Object.defineProperty(this, 'zerofish', { value: zerofish });
    Object.defineProperty(this, 'assetDb', { value: assetDb });
    Object.defineProperty(this, 'openings', {
      get: () => Promise.all(this.books ? [...this.books.map(b => this.assetDb.getBook(b.name))] : []),
    });
    Object.defineProperty(this, 'stats', { value: { moves: 0, cpl: 0 } });
  }

  get ratingText(): string {
    return `${this.glicko?.r ?? 1500}${(this.glicko?.rd ?? 3580) > 80 ? '?' : ''}`;
  }

  get fullRatingText(): string {
    return this.ratingText + ` (${Math.round(this.glicko?.rd ?? 350)})`;
  }

  get statsText(): string {
    return this.stats.moves ? `acpl ${Math.round(this.stats.cpl / this.stats.moves)}` : '';
  }

  thinking(secondsRemaining: number): number {
    return Math.min(Math.random(), secondsRemaining / 5);
  }

  async move(pos: Position, chess: co.Chess): Promise<Uci> {
    const opening = await this.bookMove(chess);
    if (opening) return opening;
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
    console.log('fishResult', fishResult);
    const { move, cpl } = zerofishMove(fishResult, zeroResult, this.operators ?? {}, chess);
    if (!isNaN(cpl)) {
      this.stats.moves++; // debug stats
      this.stats.cpl += cpl;
    }
    return move;
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
