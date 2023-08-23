import makeZerofish, { type Zerofish } from 'zerofish';
import { type Libot, botNetUrl, localBots } from '../main';

export class BabyBot implements Libot {
  name = localBots.babyBot.name;
  description = localBots.babyBot.description;
  image = localBots.babyBot.image;
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
