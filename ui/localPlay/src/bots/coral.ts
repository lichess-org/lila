import makeZerofish, { Zerofish, PV } from 'zerofish';
import { Bot } from '../bot';

export class CoralBot implements Bot {
  name: 'Coral';
  description: 'Coral is a simple bot that plays random moves.';
  image: '/lifat/bots/images/coral.webp';
  weightsUrl: '/lifat/bots/weights/maia1100.pb';

  zf: Zerofish;
  constructor() {
    makeZerofish({ weightsUrl }).then(zf => this.setZf(zf));
  }
  setZf(zf: Zerofish) {
    this.zf = zf;
    this.zf;
  }
  move(fen: string) {}
}
