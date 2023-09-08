import { type Zerofish } from 'zerofish';
import { type Libot, botNetUrl, localBots } from '../main';

export class ElsieZero implements Libot {
  name = localBots.elsieZero.name;
  description = localBots.elsieZero.description;
  image = localBots.elsieZero.image;
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
