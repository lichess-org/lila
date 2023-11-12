import { type Zerofish } from 'zerofish';
import { Libot } from '../interfaces';
import { registry } from '../ctrl';

export class Greta implements Libot {
  name = 'Greta';
  uid = '#greta';
  ordinal = 15;
  description = 'Greta is a bot that plays random moves.';
  imageUrl = lichess.assetUrl('lifat/bots/images/greta.webp', { noVersion: true });
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

registry.greta = Greta;
