import { type Zerofish } from 'zerofish';
import { type Libot, botNetUrl, localBots } from '../main';

export class BabyHoward implements Libot {
  name = localBots.babyHoward.name;
  description = localBots.babyHoward.description;
  image = localBots.babyHoward.image;
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
