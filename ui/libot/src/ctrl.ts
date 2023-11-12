import { type Zerofish } from 'zerofish';
import { Libot, Libots } from './interfaces';

export interface Ctrl {
  zf: Zerofish;
  bots: { [id: string]: Libot };
  setBot(name: string): Promise<void>;
  sort(): Libot[];
  move(fen: string): Promise<string>;
}

type Constructor<T> = new (...args: any[]) => T;

export const registry: { [k: string]: Constructor<Libot> } = {};

export async function makeCtrl(libots: Libots, zf: Zerofish): Promise<Ctrl> {
  const nets = new Map<string, Uint8Array>();
  let bot: Libot;
  return {
    zf,
    sort: libots.sort,
    bots: libots.bots,
    async setBot(id: string) {
      bot = libots.bots[id];
      if (!bot.netName) throw new Error(`unknown bot ${id} or no net`);
      if (zf.netName !== bot.netName) {
        if (!nets.has(bot.netName)) {
          nets.set(bot.netName, await fetchNet(bot.netName));
        }
        zf.setNet(id, nets.get(bot.netName)!);
        zf.netName = bot.netName;
      }
    },
    move(fen: string) {
      return bot.move(fen);
    },
  };
}

async function fetchNet(netName: string): Promise<Uint8Array> {
  return fetch(lichess.assetUrl(`lifat/bots/weights/${netName}.pb`, { noVersion: true }))
    .then(res => res.arrayBuffer())
    .then(buf => new Uint8Array(buf));
}
