import makeZerofish, { type Zerofish, type Position, type FishSearch } from 'zerofish';
import { Libot, Libots, Result, Matchup } from './types';
import { ZerofishBot, ZerofishBots } from './zerofishBot';
import { RankBot } from './rankBots';
import { ObjectStorage, objectStorage } from 'common/objectStorage';
import { clamp, defined } from 'common';

const CACHE_SIZE = 2;

export type Net = {
  name: string;
  data: Uint8Array;
};

export class BotCtrl {
  bots: Libots = {};
  rankBots: RankBot[] = [];
  net: { store?: ObjectStorage<Uint8Array>; recent: Net[] } = { recent: [] };
  players: { white?: Libot; black?: Libot } = {};
  store: ObjectStorage<Libot>;
  zf: Zerofish;

  constructor() {}

  async init() {
    [this.zf, this.net.store, this.bots] = await Promise.all([
      makeZerofish({
        root: site.asset.url('npm', { documentOrigin: true }),
        wasm: site.asset.url('npm/zerofishEngine.wasm'),
      }),
      objectStorage<Uint8Array>({ store: 'local.weights' }),
      this.fetchLibots(),
    ]);
    this.zf.fish(`setoption name Threads value ${navigator.hardwareConcurrency - 1}`, 1);
    for (let i = 0; i <= RankBot.MAX_LEVEL; i++) {
      this.rankBots.push(new RankBot(this.zf, i));
    }
    return this;
  }

  async fetchLibots(): Promise<Libots> {
    const [jsonBots, overrides] = await Promise.all([
      fetch(site.asset.url('bots.json')).then(x => x.json()),
      objectStorage<Libot>({ store: 'local.bots' }).then(s => {
        this.store = s;
        return s.getMany();
      }),
    ]);
    const bots: Libots = {};
    for (const bot of [...jsonBots, ...overrides]) {
      bots[bot.uid] = new ZerofishBot(bot, this);
    }
    return bots;
  }

  get zerofishBots(): ZerofishBots {
    return Object.fromEntries(
      Object.entries(this.bots).filter((e): e is [string, ZerofishBot] => e[1] instanceof ZerofishBot),
    );
  }

  setPlayer(color: Color, uid?: string | number) {
    this.players[color] = defined(uid) ? this.bot(uid) : undefined;
  }

  bot(uid: string | number | undefined) {
    if (uid === undefined) return;
    return this.bots[uid] ?? this.rankBot(uid);
  }

  swap() {
    [this.players.white, this.players.black] = [this.players.black, this.players.white];
  }

  move(pos: Position, color: Color): Promise<string> {
    return this.players[color]?.move(pos) ?? Promise.resolve('0000');
  }

  stop() {
    this.zf.stop();
  }

  update(bot: Libot) {
    this.store.put(bot.uid, bot);
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
    if (this.net.recent.length > CACHE_SIZE) this.net.recent.shift();
    return net.data;
  };

  private rankBot(uid: string | number) {
    const index = typeof uid === 'string' ? Number(uid.slice(1)) : uid;
    return isNaN(index) ? undefined : this.rankBots[index];
  }
}
