import makeZerofish, { type Zerofish } from 'zerofish';
import { type AssetDb } from './assetDb';
import { ZerofishBot, type ZerofishBots } from './zerofishBot';
import { RateBot } from './dev/rateBot';
import { type CardData } from './handOfCards';
import { type ObjectStorage, objectStorage } from 'common/objectStorage';
import { deepFreeze } from 'common';
import { deepScore } from './util';
import type {
  Libot,
  Libots,
  BotInfo,
  BotInfos,
  ZerofishBotInfo,
  Glicko,
  SoundEvent,
  MoveArgs,
  MoveResult,
} from './types';

export class BotCtrl {
  readonly rateBots: RateBot[] = [];
  private zerofish: Zerofish;
  private store: ObjectStorage<BotInfo>;
  private defaultBots: BotInfos;
  private busy = false;
  private bestMove = { uci: 'e2e4', cp: 30 };
  //private whiteUid?: string;
  //blackUid?: string;
  readonly uids: { white: string | undefined; black: string | undefined } = {
    white: undefined,
    black: undefined,
  };
  bots: Libots = {};

  constructor(readonly assetDb: AssetDb) {}

  get white(): Libot | undefined {
    return this.bot(this.uids.white);
  }

  get black(): Libot | undefined {
    return this.bot(this.uids.black);
  }

  get isBusy(): boolean {
    return this.busy;
  }

  async init(serverBots: BotInfo[]): Promise<this> {
    this.zerofish = await makeZerofish({
      root: site.asset.url('npm', { documentOrigin: true }),
      wasm: site.asset.url('npm/zerofishEngine.wasm'),
      dev: this.assetDb.dev,
    });
    if (this.assetDb.dev) {
      for (let i = 0; i <= RateBot.MAX_LEVEL; i++) {
        this.rateBots.push(new RateBot(this.zerofish, i));
      }
    }
    return this.initLibots(serverBots);
  }

  async initLibots(serverBots: BotInfo[]): Promise<this> {
    await Promise.all([this.resetBots(serverBots), this.assetDb.init()]);
    return this;
  }

  async localJson(): Promise<string> {
    return JSON.stringify(await this.store.getMany(), null, 2);
  }

  async clearLocalBots(uids?: string[]): Promise<void> {
    await (uids ? Promise.all(uids.map(uid => this.store.remove(uid))) : this.store.clear());
    return this.resetBots();
  }

  get zerofishBots(): ZerofishBots {
    return Object.fromEntries(
      Object.entries(this.bots).filter((e): e is [string, ZerofishBot] => e[1] instanceof ZerofishBot),
    );
  }

  setUid(c: Color, uid: string | undefined): void {
    this.uids[c] = uid;
  }

  setUids({ white, black }: { white?: string | undefined; black?: string | undefined }): void {
    this.uids.white = white;
    this.uids.black = black;
  }

  bot(uid: string | undefined): Libot | undefined {
    if (uid === undefined) return;
    return this.bots[uid] ?? this.rankBot(uid);
  }

  async move(args: MoveArgs): Promise<MoveResult | undefined> {
    if (this.busy) return undefined; // just ignore requests from different call stacks
    this.busy = true;

    const move = await this[args.chess.turn]?.move({ ...args, score: this.bestMove.cp });
    const best = (await this.zerofish.goFish(args.pos, { multipv: 1, by: { depth: 16 } })).lines[0];
    this.bestMove = { uci: best.moves[0], cp: deepScore(best) };
    this.busy = false;
    return move?.uci !== '0000' ? move : undefined;
  }

  playSound(c: Color, events: SoundEvent[]): number {
    const prioritized = soundPriority.filter(e => events.includes(e));
    const sounds = prioritized.map(priority => this[c]?.sounds?.[priority] ?? []);
    for (const set of sounds) {
      let r = Math.random();
      for (const { key, chance, delay, mix } of set) {
        r -= chance / 100;
        if (r > 0) continue;
        site.sound
          .load(key, this.assetDb.getSoundUrl(key))
          .then(() => setTimeout(() => site.sound.play(key, Math.min(1, mix * 2)), delay * 1000));
        return Math.min(1, (1 - mix) * 2);
      }
    }
    return 1;
  }

  stop(): void {
    return this.zerofish.stop();
  }

  reset(): void {
    this.bestMove = { uci: 'e2e4', cp: 30 };
    return this.zerofish.reset();
  }

  updateBot(bot: ZerofishBot): Promise<IDBValidKey> {
    this.bots[bot.uid] = new ZerofishBot(bot, this.zerofish, this.assetDb);
    return this.storeBot(bot.uid);
  }

  storeBot(uid: string): Promise<IDBValidKey> {
    return this.store.put(uid, this.bots[uid]);
  }

  defaultBot(uid: string = ''): BotInfo {
    return uid in this.defaultBots ? this.defaultBots[uid] : this.default;
  }

  async deleteBot(uid: string): Promise<void> {
    Object.entries(this.uids).forEach(([c, u]) => u === uid && this.setUid(c as Color, undefined));
    await this.store.remove(uid);
    delete this.bots[uid];
    await this.resetBots();
  }

  updateRating(bot: Libot | undefined, score: number, opp: Glicko | undefined = { r: 1500, rd: 350 }): void {
    if (!bot || bot instanceof RateBot) return;
    const q = Math.log(10) / 400;
    const glicko = bot.glicko ?? { r: 1500, rd: 350 };
    const expected = 1 / (1 + 10 ** ((opp.r - glicko.r) / 400));
    const g = 1 / Math.sqrt(1 + (3 * q ** 2 * opp.rd ** 2) / Math.PI ** 2);
    const dSquared = 1 / (q ** 2 * g ** 2 * expected * (1 - expected));
    const deltaR = (q * g * (score - expected)) / (1 / dSquared + 1 / glicko.rd ** 2);
    bot.glicko = {
      r: Math.round(glicko.r + deltaR),
      rd: Math.max(30, Math.sqrt(1 / (1 / glicko.rd ** 2 + 1 / dSquared))),
    };
    this.storeBot(bot.uid);
  }

  imageUrl(bot: BotInfo | undefined): string | undefined {
    return bot?.image && this.assetDb.getImageUrl(bot.image);
  }

  card(bot: BotInfo | undefined): CardData | undefined {
    return (
      bot && {
        label: bot.name,
        domId: uidToDomId(bot.uid)!,
        imageUrl: this.imageUrl(bot),
      }
    );
  }

  private async resetBots(defBots?: BotInfo[]) {
    const [serverBots, overrides] = await Promise.all([
      defBots ??
        fetch('/local/list')
          .then(res => res.json())
          .then(res => res.bots),
      this.getLocalOverrides(),
    ]);
    this.defaultBots = {};
    serverBots.forEach((b: BotInfo) => (this.defaultBots[b.uid] = deepFreeze(b)));
    for (const b of [...serverBots, ...overrides]) {
      if (!b.name) delete this.bots[b.uid];
      this.bots[b.uid] = new ZerofishBot(b, this.zerofish, this.assetDb);
    }
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
    return this.rateBots[Number(uid.slice(1))];
  }

  readonly default: ZerofishBotInfo = deepFreeze({
    uid: '#default',
    name: 'Name',
    description: 'Description',
    image: 'gray-torso.webp',
    books: [],
    fish: { multipv: 1, by: { depth: 1 } },
    version: 0,
  });
}

export function uidToDomId(uid: string | undefined): string | undefined {
  return uid?.startsWith('#') ? `bot-id-${uid.slice(1)}` : undefined;
}

export function domIdToUid(domId: string | undefined): string | undefined {
  return domId && domId.startsWith('bot-id-') ? `#${domId.slice(7)}` : undefined;
}

const soundPriority: SoundEvent[] = [
  'playerWin',
  'botWin',
  'playerCheck',
  'botCheck',
  'playerCapture',
  'botCapture',
  'playerMove',
  'botMove',
  'greeting',
];
