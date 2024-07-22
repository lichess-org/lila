import * as co from 'chessops';
import makeZerofish, { type Zerofish, type Position } from 'zerofish';
import { AssetDb } from './assetDb';
import { ZerofishBot, ZerofishBots } from './zerofishBot';
import { RankBot } from './dev/rankBot';
import { CardData } from './handOfCards';
import { type ObjectStorage, objectStorage } from 'common/objectStorage';
import { deepFreeze } from 'common';
import type { Libot, Libots, BotInfo, BotInfos, ZerofishBotInfo, Glicko, SoundEvent, Sound } from './types';

export class BotCtrl {
  readonly rankBots: RankBot[] = [];
  private zerofish: Zerofish;
  private store: ObjectStorage<BotInfo>;
  private defaultBots: BotInfos;
  bots: Libots;
  whiteUid?: string;
  blackUid?: string;

  constructor(readonly assetDb: AssetDb) {}

  async init(): Promise<this> {
    this.zerofish = await makeZerofish({
      root: site.asset.url('npm', { documentOrigin: true }),
      wasm: site.asset.url('npm/zerofishEngine.wasm'),
      dev: this.assetDb.dev,
    });
    if (this.assetDb.dev) {
      for (let i = 0; i <= RankBot.MAX_LEVEL; i++) {
        this.rankBots.push(new RankBot(this.zerofish, i));
      }
    }
    return this.initLibots();
  }

  async initLibots(): Promise<this> {
    await Promise.all([this.resetBots(), this.assetDb.init()]);
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

  get white(): Libot | undefined {
    return this.bot(this.whiteUid);
  }

  get black(): Libot | undefined {
    return this.bot(this.blackUid);
  }

  bot(uid: string | undefined): Libot | undefined {
    if (uid === undefined) return;
    return this.bots[uid] ?? this.rankBot(uid);
  }

  move(pos: Position, chess: co.Chess): Promise<Uci> {
    return this[chess.turn]?.move(pos, chess) ?? Promise.resolve('0000');
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
    this.zerofish.stop();
  }

  reset(): void {
    this.zerofish.reset();
  }

  updateBot(bot: ZerofishBot): Promise<IDBValidKey> {
    this.bots[bot.uid] = new ZerofishBot(bot, this.zerofish, this.assetDb);
    return this.saveBot(bot.uid);
  }

  saveBot(uid: string): Promise<IDBValidKey> {
    return this.store.put(uid, this.bots[uid]);
  }

  defaultBot(uid: string = ''): BotInfo {
    return uid in this.defaultBots ? this.defaultBots[uid] : this.default;
  }

  updateRating(bot: Libot | undefined, score: number, opp: Glicko | undefined = { r: 1500, rd: 350 }): void {
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

  private async resetBots() {
    const [jsonBots, overrides] = await Promise.all([
      fetch(site.asset.url('json/local.bots.json')).then(x => x.json()),
      this.getLocalOverrides(),
    ]);
    this.defaultBots = {};
    jsonBots.forEach((b: BotInfo) => (this.defaultBots[b.uid] = deepFreeze(b)));
    this.bots = {};
    for (const b of [...jsonBots, ...overrides]) {
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
    return this.rankBots[Number(uid.slice(1))];
  }

  readonly default: ZerofishBotInfo = deepFreeze({
    uid: '#default',
    name: 'Name',
    description: 'Description',
    image: 'gray-torso.webp',
    books: [], //[{ name: 'gm2600.bin', weight: 1 }],
    fish: { multipv: 1, by: { depth: 1 } },
    //zero: { multipv: 1, net: 'maia-1100.pb' },
    //glicko: { r: 1500, rd: 350 },
  });
}

export function botAssetUrl(name: string, version: string | false = 'bot000'): string {
  return site.asset.url(`lifat/bots/${name}`, { version });
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
