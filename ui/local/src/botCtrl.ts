import makeZerofish, { type Zerofish, type Position, type FishSearch } from 'zerofish';
import { Libot, Libots, BotInfo, BotInfos } from './types';
import { AssetDb } from './assetDb';
import { ZerofishBot, ZerofishBots } from './zerofishBot';
import { RankBot } from './dev/rankBot';
import { PolyglotBook } from 'bits/types';
import { ObjectStorage, objectStorage } from 'common/objectStorage';
import * as co from 'chessops';
import { clamp, defined } from 'common';

export class BotCtrl {
  readonly assetDb: AssetDb;
  private botDefaults: BotInfos;
  bots: Libots;
  rankBots: RankBot[] = [];
  white?: Libot;
  black?: Libot;
  store: ObjectStorage<Libot>;
  zf: Zerofish;

  constructor(assets?: { nets: string[]; images: string[]; books: string[] }) {
    this.assetDb = new AssetDb(assets);
  }

  async init() {
    [this.zf, this.bots] = await Promise.all([
      makeZerofish({
        root: site.asset.url('npm', { documentOrigin: true }),
        wasm: site.asset.url('npm/zerofishEngine.wasm'),
        dev: this.assetDb.assets !== undefined,
      }),
      this.initLibots(),
    ]);

    for (let i = 0; i <= RankBot.MAX_LEVEL; i++) {
      this.rankBots.push(new RankBot(this.zf, i));
    }
    return this;
  }

  async initLibots(): Promise<Libots> {
    await Promise.all([this.resetBots(), this.assetDb.ready]);
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

  bot(uid: string | undefined): Libot | undefined {
    if (uid === undefined) return;
    return this.bots[uid] ?? this.rankBot(uid);
  }

  swap() {
    [this.white, this.black] = [this.black, this.white];
  }

  move(pos: Position, chess: co.Chess): Promise<string> {
    chess = chess.clone();
    pos = structuredClone(pos);
    return this[chess.turn]?.move(pos, chess) ?? Promise.resolve('0000');
  }

  stop() {
    this.zf.stop();
  }

  reset() {
    this.zf.reset();
  }

  setBot(bot: ZerofishBot) {
    this.bots[bot.uid] = new ZerofishBot(bot, this);
    this.store.put(bot.uid, bot);
  }

  botDefault(uid: string = ''): BotInfo {
    return uid in this.botDefaults ? structuredClone(this.botDefaults[uid]) : structuredClone(this.default);
  }

  private async resetBots() {
    const [jsonBots, overrides] = await Promise.all([
      fetch(site.asset.url('json/local.bots.json')).then(x => x.json()),
      this.getLocalOverrides(),
    ]);
    this.botDefaults = {};
    jsonBots.forEach((b: BotInfo) => (this.botDefaults[b.uid] = b));
    this.bots = {};
    [...jsonBots, ...overrides].forEach((b: Libot) => (this.bots[b.uid] = new ZerofishBot(b, this)));
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

  readonly default: BotInfo = {
    uid: '#default',
    name: 'Name',
    description: 'Description',
    image: 'gray-torso.webp',
    books: [{ name: 'gm2600.bin', weight: 1 }],
    glicko: { r: 1500, rd: 350 },
    //zero: { multipv: 1, net: 'maia-1100.pb' },
    fish: { multipv: 1, by: { depth: 1 } },
  };
}

export function botAssetUrl(name: string, version: string | false = 'bot000') {
  return site.asset.url(`lifat/bots/${name}`, { version });
}
