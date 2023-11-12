import { type Zerofish } from 'zerofish';
import { Libot } from '../interfaces';
import { registry } from '../ctrl';

export class Coral implements Libot {
  name = 'Coral';
  uid = '#coral';
  ordinal = 2;
  description = 'Coral is a bot that plays random moves.';
  imageUrl = lichess.assetUrl('lifat/bots/images/coral.webp', { noVersion: true });
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

registry.coral = Coral;
