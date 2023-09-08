import { type Zerofish } from 'zerofish';
import { Libot, botNetUrl, localBots } from '../main';

export class Beatrice implements Libot {
  name = localBots.beatrice.name;
  description = localBots.beatrice.description;
  image = localBots.beatrice.image;
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
