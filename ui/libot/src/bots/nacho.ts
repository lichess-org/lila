import { type Zerofish } from 'zerofish';
import { Libot } from '../interfaces';
import { registry } from '../ctrl';

export class Nacho implements Libot {
  name = 'Nacho';
  uid = '#nacho';
  ordinal = 4;
  description = 'Nacho is a bot that plays random moves.';
  imageUrl = lichess.assetUrl('lifat/bots/images/nacho.webp', { noVersion: true });
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

registry.nacho = Nacho;
