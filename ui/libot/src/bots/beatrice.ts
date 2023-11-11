import { type Zerofish } from 'zerofish';
import { Libot } from '../interfaces';
import { registry } from '../ctrl';

export class Beatrice implements Libot {
  name = 'Beatrice';
  description = 'Beatrice is a bot that plays random moves.';
  imageUrl = lichess.assetUrl('lifat/bots/images/beatrice.webp', { noVersion: true });
  netName = 'maia-1100';
  ratings = new Map();
  zf: Zerofish;

  constructor(zf: Zerofish, opts?: any) {
    opts;
    this.zf = zf;
  }

  async move(fen: string) {
    return await this.zf.goZero(fen);
  }
}

registry.beatrice = Beatrice;
