import type { Zerofish, ZeroSearch, FishSearch, Position } from 'zerofish';
import { Libot, ZfBotConfig, CardData } from './types';
import { BotCtrl } from './botCtrl';

export type ZerofishBots = { [id: string]: ZerofishBot };

export class ZerofishBot implements Libot {
  name: string;
  readonly uid: string;
  description: string;
  image: { lifat?: string; url?: string };
  zero?: { netName: string; depth?: number };
  fish?: { search?: FishSearch };
  card?: CardData;
  rating?: number;
  ctrl: BotCtrl;

  constructor(info: Libot, ctrl: BotCtrl) {
    Object.assign(this, info);
    if (!this.card)
      this.card = {
        label: this.name,
        domId: this.uid.startsWith('#') ? this.uid.slice(1) : this.uid,
        imageUrl: this.imageUrl,
      };
    Object.defineProperty(this, 'ctrl', {
      value: ctrl,
      enumerable: false,
    });
  }

  get imageUrl() {
    return this.image?.url ?? site.asset.url(`lifat/bots/images/${this.image.lifat}`, { version: 'bot000' });
  }

  set imageUrl(url: string) {
    this.image = { url };
    if (this.card) this.card.imageUrl = url;
  }

  async move(pos: Position) {
    const promises = [];
    if (this.zero) {
      promises.push(
        this.ctrl.zf.goZero(pos, {
          depth: this.zero.depth,
          net: { name: this.zero.netName, fetch: this.ctrl.getNet },
        }),
      );
    }
    if (this.fish) {
      promises.push(await this.ctrl.zf.goFish(pos, this.fish.search));
    }
    const movers = await Promise.all(promises);
    if (movers.length === 0) return '0000';
    else if (movers.length === 1) return movers[0].bestmove;
    else {
      const [zeroResult, fishResult] = movers;
      return this.chooseMove(pos.fen!, zeroResult, fishResult);
    }
  }

  updateRating(rating: number) {
    this.rating = Math.round(rating / 10) * 10;
    this.ctrl.update(this);
    return rating;
  }

  update() {
    this.ctrl.update(this);
  }

  chooseMove(fen: string, zeroResult: any, fishResult: any) {
    return zeroResult.bestmove;
  }
}
