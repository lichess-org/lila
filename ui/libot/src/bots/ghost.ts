import { type Zerofish } from 'zerofish';
import { Libot } from '../interfaces';
import { registry } from '../ctrl';

export class Ghost implements Libot {
  name = 'Ghost';
  description = 'Ghost is a bot that plays random moves.';
  imageUrl = lichess.assetUrl('lifat/bots/images/specops-lady.webp', { noVersion: true });
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

registry.ghost = Ghost;
