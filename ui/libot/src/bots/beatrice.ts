import makeZerofish, { type Zerofish } from 'zerofish';
import { Libot, botNetUrl, localBots } from '../main';

export class Beatrice implements Libot {
  name = localBots.beatrice.name;
  description = localBots.beatrice.description;
  image = localBots.beatrice.image;
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
