import { type Zerofish } from 'zerofish';
import { Libot, botNetUrl, localBots } from '../main';

export class Coral implements Libot {
  name = localBots.coral.name;
  description = localBots.coral.description;
  image = localBots.coral.image;
  net = 'maia-1100';
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
