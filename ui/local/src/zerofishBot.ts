import * as co from 'chessops';
import { normalize } from './operator';
import { zerofishMove } from './zerofishMove';
import type { FishSearch, Position } from 'zerofish';
import type { Libot, ZerofishBotInfo, CardData, ZeroSearch, Operator } from './types';
import type { PolyglotBook } from 'bits/types';
import type { BotCtrl } from './botCtrl';

export type ZerofishBots = { [id: string]: ZerofishBot };

export class ZerofishBot implements Libot, ZerofishBotInfo {
  private ctrl: BotCtrl;
  private openings: Promise<PolyglotBook[]>;
  private stats: any;
  readonly uid: string;
  name: string;
  description: string;
  image: string;
  books: { name: string; weight?: number }[] = [];
  zero?: ZeroSearch;
  fish?: FishSearch;
  glicko?: { r: number; rd: number };
  operators?: { [type: string]: Operator };

  constructor(info: Libot, ctrl: BotCtrl) {
    Object.assign(this, info);
    Object.values(this.operators ?? {}).forEach(normalize);

    // non enumerable properties are not stored or cloned
    Object.defineProperty(this, 'ctrl', { value: ctrl });
    Object.defineProperty(this, 'openings', {
      get: () => Promise.all(this.books ? [...this.books.map(b => ctrl.assetDb.getBook(b.name))] : []),
    });
    Object.defineProperty(this, 'stats', { value: { moves: 0, cpl: 0 } });
  }

  get imageUrl() {
    return this.ctrl.assetDb.getImageUrl(this.image);
  }

  async bookMove(chess: co.Chess) {
    if (!this.books?.length) return undefined;
    const moveList: { moves: { uci: Uci; weight: number }[]; bookWeight: number }[] = [];
    let bookChance = 0;
    const openings = await this.openings;
    for (let i = 0; i < this.books.length; i++) {
      const moves = openings[i](chess);
      if (moves.length === 0) continue;
      const bookWeight = this.books[i].weight ?? 1;
      moveList.push({ moves, bookWeight });
      bookChance += bookWeight;
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

  async move(pos: Position, chess: co.Chess) {
    const opening = await this.bookMove(chess);
    if (opening) return opening;
    const [zeroResult, fishResult] = await Promise.all([
      this.zero &&
        this.ctrl.zf.goZero(pos, {
          ...this.zero,
          net: {
            name: this.name + '-' + this.zero.net,
            fetch: async () => (await this.ctrl.assetDb.getNet(this.zero!.net))!,
          },
        }),
      this.fish && this.ctrl.zf.goFish(pos, this.fish),
    ]);
    const { move, cpl } = zerofishMove(fishResult, zeroResult, this.operators ?? {}, chess);
    if (!isNaN(cpl)) {
      this.stats.moves++; // debug stats
      this.stats.cpl += cpl;
    }
    return move;
  }

  get ratingText() {
    return `${this.glicko?.r ?? 1500}${(this.glicko?.rd ?? 3580) > 80 ? '?' : ''}`;
  }

  get fullRatingText() {
    return this.ratingText + ` (${Math.round(this.glicko?.rd ?? 350)})`;
  }

  get statsText() {
    return this.stats.moves ? `acpl ${Math.round(this.stats.cpl / this.stats.moves)}` : '';
  }
}
