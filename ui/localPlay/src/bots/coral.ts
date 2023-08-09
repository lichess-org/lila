import makeZerofish, { type Zerofish } from 'zerofish';
import { Bot, netUrl } from '../bot';

export class CoralBot implements Bot {
  name = 'Coral';
  description = 'Coral is a simple bot that plays random moves.';
  image = 'coral.webp';
  net = 'maia-1100.pb';
  ratings = new Map();

  zf: Zerofish;
  constructor() {
    makeZerofish({ pbUrl: netUrl(this.net) }).then(zf => this.setZf(zf));
  }
  setZf(zf: Zerofish) {
    this.zf = zf;
    this.zf;
  }
  async move(fen: string) {
    return await this.zf.goZero(fen);
  }
}
