import makeZerofish, { type Zerofish } from 'zerofish';
import { Libot, botNetUrl, localBots } from '../main';

export class Coral implements Libot {
  name = localBots.coral.name;
  description = localBots.coral.description;
  image = localBots.coral.image;
  net = botNetUrl('maia-1100.pb');
  ratings = new Map();
  zf: Zerofish;

  constructor(opts?: any) {
    opts;
    makeZerofish({ pbUrl: this.net }).then(zf => (this.zf = zf));
  }

  async move(fen: string) {
    return await this.zf.goZero(fen);
  }
}
