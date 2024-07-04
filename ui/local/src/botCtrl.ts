import makeZerofish, { type Zerofish, type Position, type FishSearch } from 'zerofish';
import { Libot, Libots, BotInfo, BotInfos, ZerofishBotInfo, CardData } from './types';
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
  store: ObjectStorage<BotInfo>;
  zf: Zerofish;

  constructor(assets?: { nets: string[]; images: string[]; books: string[] }) {
    this.assetDb = new AssetDb(assets);
  }

  async init() {
    [this.zf] = await Promise.all([
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

  async initLibots() {
    await Promise.all([this.resetBots(), this.assetDb.ready]);
    return this;
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

  updateBot(bot: ZerofishBot) {
    this.bots[bot.uid] = new ZerofishBot(bot, this);
    this.saveBot(bot.uid);
  }

  saveBot(uid: string) {
    return this.store.put(uid, this.bots[uid]);
  }

  defaultBot(uid: string = ''): BotInfo {
    return uid in this.botDefaults ? structuredClone(this.botDefaults[uid]) : structuredClone(this.default);
  }

  updateRating(bot: Libot | undefined, opp: { r: number; rd: number } = { r: 1500, rd: 350 }, score: number) {
    if (!bot || bot instanceof RankBot) return;
    const q = Math.log(10) / 400;
    bot.glicko ??= { r: 1500, rd: 350 };
    const expected = 1 / (1 + 10 ** ((opp.r - bot.glicko.r) / 400));
    const g = 1 / Math.sqrt(1 + (3 * q ** 2 * opp.rd ** 2) / Math.PI ** 2);
    const dSquared = 1 / (q ** 2 * g ** 2 * expected * (1 - expected));
    const deltaR = (q * g * (score - expected)) / (1 / dSquared + 1 / bot.glicko.rd ** 2);
    bot.glicko = {
      r: Math.round(bot.glicko.r + deltaR),
      rd: Math.max(30, Math.sqrt(1 / (1 / bot.glicko.rd ** 2 + 1 / dSquared))),
    };
    this.saveBot(bot.uid);
  }

  imageUrl(bot: BotInfo | undefined) {
    if (!bot?.image) return;
    return this.assetDb.getImageUrl(bot.image);
  }

  card(bot: BotInfo | undefined) {
    if (!bot) return;
    return {
      label: bot.name,
      domId: uidToDomId(bot.uid)!,
      imageUrl: this.imageUrl(bot),
    };
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
      objectStorage<BotInfo>({ store: 'local.bots' }).then(s => {
        this.store = s;
        return s.getMany();
      })
    );
  }

  private rankBot(uid: string) {
    const index = Number(uid.slice(1));
    return isNaN(index) ? undefined : this.rankBots[index];
  }

  readonly default: ZerofishBotInfo = {
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

export function uidToDomId(uid: string | undefined) {
  return uid?.startsWith('#') ? `bot-id-${uid.slice(1)}` : undefined;
}

export function domIdToUid(domId: string | undefined) {
  return domId && domId.length > 7 ? `#${domId.slice(7)}` : undefined;
}
