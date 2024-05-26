import { ObjectStorage, objectStorage } from 'common/objectStorage';
import { GameState } from './playCtrl';

export async function makeDatabase(netCacheSize: number): Promise<Database> {
  return await new Database(netCacheSize).init();
}

export class Database {
  net: { store?: ObjectStorage<Uint8Array>; recent: Net[] };
  gameState: ObjectStorage<GameState>;

  constructor(readonly netCacheSize: number) {
    this.net = { store: undefined, recent: [] };
  }

  async init() {
    [this.net.store, this.gameState] = await Promise.all([
      objectStorage<Uint8Array>({ store: 'local.weights' }),
      objectStorage<GameState>({ store: 'local.play' }),
    ]);
    return this;
  }

  async getState(): Promise<GameState> {
    return this.gameState.get('current');
  }

  async putState(state: GameState) {
    this.gameState.put('current', state);
  }

  getNet = async (netName: string): Promise<Uint8Array> => {
    const cached = this.net.recent.find(n => n.name === netName);
    if (cached) return cached.data;
    let netData = await this.net.store?.get(netName);
    if (!netData) {
      netData = await fetch(site.asset.url(`lifat/bots/weights/${netName}`, { version: false }))
        .then(res => res.arrayBuffer())
        .then(buf => new Uint8Array(buf));
      this.net.store?.put(netName, netData!);
    }
    const net = { name: netName, data: netData! };
    this.net.recent.push(net);
    if (this.net.recent.length > this.netCacheSize) this.net.recent.shift();
    return net.data;
  };
}

export type Net = {
  name: string;
  data: Uint8Array;
};
