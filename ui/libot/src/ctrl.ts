import makeZerofish, { type Zerofish } from 'zerofish';
import { Libot } from './interfaces';
import { Coral } from './bots/coral';
import { BabyHoward } from './bots/babyHoward';
import { ElsieZero } from './bots/elsieZero';
import { Beatrice } from './bots/beatrice';

export interface Ctrl {
  zf: Zerofish;
  setBot(name: string): Promise<void>;
  move(fen: string): Promise<string>;
}

export async function makeCtrl(): Promise<Ctrl> {
  const zf = await makeZerofish();
  const nets = new Map<string, Uint8Array>();
  const bots: { [k: string]: Libot } = {
    coral: new Coral(zf),
    babyHoward: new BabyHoward(zf),
    elsieZero: new ElsieZero(zf),
    beatrice: new Beatrice(zf),
  };

  return {
    zf,
    setBot(name: string) {
      const net = bots[name]?.net;
      if (!net) throw new Error(`unknown bot ${name} or no net`);
      if (zf.netName !== bots[name].net) {
        if (!nets.has(bots[name].net)) {
          nets.set(bots[name].net, fetchNet(bots[name].net));
        }
        return nets.get(bots[name].net).then(buf => {
          zf.setNet(buf);
          zf.netName = bots[name].net;
        });
      }
    },
    move(fen: string) {
      return zf.goZero(fen);
    },
  };
}

function fetchNet(netName: string): Promise<Uint8Array> {
  return fetch(`/lifat/bots/weights/${netName}.pb`)
    .then(res => res.arrayBuffer())
    .then(buf => new Uint8Array(buf));
}
