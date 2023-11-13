import { type Zerofish } from 'zerofish';
import { Libot } from '../interfaces';
import { registry } from '../ctrl';

export class Owen implements Libot {
  name = 'Owen';
  uid = '#owen';
  ordinal = 2;
  description = 'Owen is a bot that plays random moves.';
  imageUrl = lichess.assetUrl('lifat/bots/images/owen.webp', { noVersion: true });
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

registry.owen = Owen;
