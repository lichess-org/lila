import { type Zerofish } from 'zerofish';
import { Libot, BotInfo } from './interfaces';

let ordinal = 0;
export class ZeroBot implements Libot {
  readonly name: string;
  readonly uid: string;
  readonly description: string;
  readonly image: string;
  readonly netName?: string;
  ratings = new Map();
  ordinal: number;
  zf: Zerofish;
  get imageUrl() {
    return lichess.assetUrl(`lifat/bots/images/${this.image}`, { noVersion: true });
  }
  constructor(info: BotInfo, zf: Zerofish) {
    Object.assign(this, info);
    this.zf = zf;
    this.ordinal = ordinal++;
  }

  async move(fen: string) {
    return await this.zf.goZero(fen);
  }
}
