import { type Zerofish } from 'zerofish';
import { Libot, Libots } from './interfaces';
import { ZfBot } from './zfbot';

export class BotCtrl {
  zf: { white: Zerofish; black: Zerofish };
  bots: Libots;
  players: { white?: Libot; black?: Libot } = {};

  constructor(inbots: any, zf: { white: Zerofish; black: Zerofish }) {
    this.zf = zf;
    this.bots = {};
    for (const bot of inbots) {
      this.bots[bot.uid.slice(1)] = new ZfBot(bot);
    }
  }

  async setBot(color: Color, uid?: string) {
    const id = nohash(uid);
    const bot = (this.players[color] = id ? this.bots[id] : undefined);

    if (bot?.netName && this.zf[color].netName !== bot.netName) {
      this.zf[color].setNet(bot.netName, await fetchNet(bot.netName)!);
      this.zf[color].netName = bot.netName;
    }
  }
  swap() {
    [this.players.white, this.players.black] = [this.players.black, this.players.white];
    [this.zf.white, this.zf.black] = [this.zf.black, this.zf.white];
  }
  move(fen: string, color: Color): Promise<string> {
    return this.players[color]?.move(fen, this.zf[color]) ?? Promise.resolve('0000');
  }
}

async function fetchNet(netName: string): Promise<Uint8Array> {
  return fetch(site.asset.url(`lifat/bots/weights/${netName}`, { version: false }))
    .then(res => res.arrayBuffer())
    .then(buf => new Uint8Array(buf));
}

function nohash(uid?: string) {
  return uid?.startsWith('#') ? uid.slice(1) : uid;
}
