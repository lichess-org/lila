import { type Zerofish } from 'zerofish';
import { Libot } from '../interfaces';
import { registry } from '../ctrl';

export class Dansby implements Libot {
  name = 'Dansby';
  uid = '#dansby';
  ordinal = 11;
  description = 'Dansby is a bot that plays random moves.';
  imageUrl = lichess.assetUrl('lifat/bots/images/dansby.webp', { noVersion: true });
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

registry.dansby = Dansby;
