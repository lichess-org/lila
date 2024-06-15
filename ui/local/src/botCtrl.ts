import makeZerofish, { type Zerofish, type Position, type FishSearch } from 'zerofish';
import { Libot, Libots, BotInfo, BotInfos, AssetLoc } from './types';
import { ZerofishBot, ZerofishBots } from './zerofishBot';
import { RankBot } from './rankBot';
import { PolyglotBook } from 'bits/types';
import { ObjectStorage, objectStorage } from 'common/objectStorage';
import * as co from 'chessops';
import { clamp, defined } from 'common';

const NET_CACHE_SIZE = 2;

export type Net = {
  name: string;
  data: Uint8Array;
};

export class BotCtrl {
  readonly default: BotInfo = defaultZerofishBot as BotInfo;
  botDefaults: BotInfos;
  bots: Libots;
  rankBots: RankBot[] = [];
  nets: { locs: { [name: string]: AssetLoc }; store?: ObjectStorage<Uint8Array>; recent: Net[] } = {
    locs: {},
    recent: [],
  };
  books = new Map<string, PolyglotBook>();
  white?: Libot;
  black?: Libot;
  store: ObjectStorage<Libot>;
  zf: Zerofish;

  constructor() {}

  async init() {
    [this.zf, this.nets.store, this.bots, this.nets.locs] = await Promise.all([
      makeZerofish({
        root: site.asset.url('npm', { documentOrigin: true }),
        wasm: site.asset.url('npm/zerofishEngine.wasm'),
        dev: true,
      }),
      objectStorage<Uint8Array>({ store: 'local.nets' }),
      this.initLibots(),
      fetch(botAssetUrl('json/local.nets.json')).then(x => x.json()),
    ]);

    for (let i = 0; i <= RankBot.MAX_LEVEL; i++) {
      this.rankBots.push(new RankBot(this.zf, i));
    }
    this.zf.fish(`setoption name Threads value ${navigator.hardwareConcurrency - 1}`, 1);
    return this;
  }

  async initLibots(): Promise<Libots> {
    await this.resetBots();
    return this.bots;
  }

  async localJson(): Promise<string> {
    return JSON.stringify(await this.store.getMany(), null, 2);
  }

  async clearLocalBots(uids?: string[]) {
    await (uids ? Promise.all(uids.map(uid => this.store.remove(uid))) : this.store.clear());
    return this.resetBots();
  }

  get zerofishBots(): ZerofishBots {
    return Object.fromEntries(
      Object.entries(this.bots).filter((e): e is [string, ZerofishBot] => e[1] instanceof ZerofishBot),
    );
  }

  setPlayer(color: Color, uid?: string) {
    this[color] = defined(uid) ? this.bot(uid) : undefined;
  }

  bot(uid: string | undefined) {
    if (uid === undefined) return;
    return this.bots[uid] ?? this.rankBot(uid);
  }

  swap() {
    [this.white, this.black] = [this.black, this.white];
  }

  move(pos: Position, chess: co.Chess): Promise<string> {
    return (
      this[chess.turn]?.move({ fen: pos.fen, moves: pos.moves?.slice(0) }, chess) ?? Promise.resolve('0000')
    );
  }

  stop() {
    this.zf.stop();
  }

  update(bot: Libot) {
    this.store.put(bot.uid, bot);
  }

  getNet = async (netName: string): Promise<Uint8Array> => {
    const cached = this.nets.recent.find(n => n.name === netName);
    if (cached) return cached.data;
    console.log(netName);
    const loc = this.nets.locs[netName];
    const netData =
      (await this.nets.store?.get(netName)) ??
      (await fetch(loc.url ?? botAssetUrl(`weights/${loc.lichess}`, false))
        .then(res => res.arrayBuffer())
        .then(buf => new Uint8Array(buf)));
    if (!netData) throw new Error(`error ${netName} {$JSON.stringify(loc)}`);
    this.nets.store?.put(netName, netData);
    this.nets.recent.push({ name: netName, data: netData! });
    if (this.nets.recent.length > NET_CACHE_SIZE) this.nets.recent.shift();
    return netData;
  };

  getBook = async (book: AssetLoc | undefined) => {
    if (!book) return;
    const url = book?.url ?? botAssetUrl(`openings/${book.lichess}`);
    if (this.books.has(url)) return this.books.get(url)!;
    const openings = await fetch(url)
      .then(res => res.arrayBuffer())
      .then(buf => site.asset.loadEsm<PolyglotBook>('bits.polyglot', { init: new DataView(buf) }));
    this.books.set(url, openings);
    return openings;
  };

  private async resetBots() {
    const [jsonBots, overrides] = await Promise.all([
      fetch(site.asset.url('json/local.bots.json')).then(x => x.json()),
      this.getLocalOverrides(),
    ]);
    this.botDefaults = {};
    jsonBots.forEach((b: BotInfo) => (this.botDefaults[b.uid] = b));
    this.bots = {};
    [...jsonBots, ...overrides].forEach((b: Libot) => (this.bots[b.uid] = new ZerofishBot(b, this)));
    Object.values(this.bots).forEach(b => {
      if (b.zero?.net) this.nets.locs[b.zero.net.lichess ?? b.zero.net.url] = b.zero.net;
    });
    console.log(this.nets.locs);
    this.white = this.white ? this.bots[this.white.uid] : undefined;
    this.black = this.black ? this.bots[this.black.uid] : undefined;
  }

  private getLocalOverrides() {
    return (
      this.store?.getMany() ??
      objectStorage<Libot>({ store: 'local.bots' }).then(s => {
        this.store = s;
        return s.getMany();
      })
    );
  }

  private rankBot(uid: string) {
    const index = Number(uid.slice(1));
    return isNaN(index) ? undefined : this.rankBots[index];
  }
}

export function botAssetUrl(name: string, version: string | false = 'bot000') {
  console.log(name, version);
  return site.asset.url(`lifat/bots/${name}`, { version });
}

const defaultZerofishBot: BotInfo = {
  uid: `#${Math.random().toString(36).slice(2)}`,
  name: 'Name',
  description: 'Description',
  image: { lichess: 'gray-torso.webp', url: undefined },
  book: { lichess: 'gm2600.bin', url: undefined },
  glicko: { r: 1500, rd: 350 },
  zero: { net: { lichess: 'maia-1100.pb', url: undefined }, search: { nodes: 1 } },
  fish: { multipv: 12, search: { depth: 12 } },
  searchMix: { by: 'moves', data: [], range: { min: 0, max: 1 } },
};
