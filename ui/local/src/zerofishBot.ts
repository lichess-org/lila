import type { Zerofish, SearchResult, Search, Position } from 'zerofish';
import { Libot, AssetLoc, CardData, Point, Mapping } from './types';
import { PolyglotBook } from 'bits/types';
import { BotCtrl, botAssetUrl } from './botCtrl';
import { interpolate, normalize } from './mapping';
import * as co from 'chessops';

export type ZerofishBots = { [id: string]: ZerofishBot };

export class ZerofishBot implements Libot {
  name: string;
  readonly uid: string;
  readonly card: CardData;
  description: string;
  image: AssetLoc;
  book?: AssetLoc;
  zero?: { net: AssetLoc; search: Search };
  fish?: { multipv: number; search: Search };
  glicko: { r: number; rd: number };
  searchMix?: Mapping;
  private ctrl: BotCtrl;
  private openings: Promise<PolyglotBook | undefined>;

  constructor(info: Libot, ctrl: BotCtrl) {
    Object.assign(this, info);
    Object.defineProperty(this, 'ctrl', {
      enumerable: false,
      value: ctrl,
    });
    Object.defineProperty(this, 'openings', {
      enumerable: false,
      get: () => ctrl.getBook(this.book),
    });
    Object.defineProperty(this, 'card', {
      enumerable: false,
      get: () => ({
        label: this.name,
        domId: this.uid.startsWith('#') ? this.uid.slice(1) : this.uid,
        imageUrl: this.imageUrl,
      }),
    });
    if (this.searchMix) normalize(this.searchMix);
  }

  get imageUrl() {
    return this.image?.url ?? botAssetUrl(`images/${this.image.lichess}`);
  }

  async move(pos: Position, chess: co.Chess) {
    const promises: Promise<SearchResult>[] = [];
    const openings = (await this.openings)?.(chess);
    let chance = Math.random();
    for (const { uci, weight } of openings ?? []) {
      chance -= weight;
      if (chance <= 0) return uci;
    }
    if (this.zero)
      promises.push(
        this.ctrl.zf.goZero(pos, {
          search: this.zero.search,
          net: { name: this.zero.net.lichess ?? this.zero.net.url, fetch: this.ctrl.getNet },
        }),
      );
    if (this.fish) promises.push(this.ctrl.zf.goFish(pos, this.fish));
    const movers = await Promise.all(promises);
    if (movers.length === 0) return '0000';
    else if (movers.length === 1) return movers[0].bestmove;
    else {
      const [zeroResult, fishResult] = movers;
      return this.chooseMove(chess, zeroResult, fishResult);
    }
  }

  updateRating(opp: { r: number; rd: number } = { r: 1500, rd: 350 }, score: number) {
    const q = Math.log(10) / 400;
    this.glicko ??= { r: 1500, rd: 350 };
    const expected = 1 / (1 + 10 ** ((opp.r - this.glicko.r) / 400));
    const g = 1 / Math.sqrt(1 + (3 * q ** 2 * opp.rd ** 2) / Math.PI ** 2);
    const dSquared = 1 / (q ** 2 * g ** 2 * expected * (1 - expected));
    const deltaR = (q * g * (score - expected)) / (1 / dSquared + 1 / this.glicko.rd ** 2);
    this.glicko = {
      r: Math.round(this.glicko.r + deltaR),
      rd: Math.max(30, Math.sqrt(1 / (1 / this.glicko.rd ** 2 + 1 / dSquared))),
    };
  }

  get ratingText() {
    return `${this.glicko?.r ?? 1500}${(this.glicko?.rd ?? 350) > 80 ? '?' : ''}`;
  }

  update() {
    this.ctrl.update(this);
  }

  chooseMove(chess: co.Chess, zeroResult: any, fishResult: any) {
    if (this.searchMix && this.searchMix.by === 'moves') {
      const fishMix = interpolate(this.searchMix, chess.fullmoves);
      return Math.random() < (fishMix ?? 0) ? fishResult.bestmove : zeroResult.bestmove;
    }
    return zeroResult.bestmove;
  }
}
