import makeZerofish, { type Zerofish, type Position } from 'zerofish';
import * as co from 'chessops';
import { type Assets } from './assets';
import { Bot } from './bot';
import { RateBot } from './dev/rateBot';
import { closeEnough } from './dev/devUtil';
import { type CardData } from './handOfCards';
import { type ObjectStorage, objectStorage } from 'common/objectStorage';
import { deepFreeze, isEquivalent } from 'common';
import { deepScore } from './util';
import type { BotInfo, Glicko, SoundEvent, Mover, MoveArgs, MoveResult, Ratings, LocalSpeed } from './types';

export class BotCtrl {
  zerofish: Zerofish;
  assets: Assets;
  serverBots: { [uid: string]: BotInfo };
  localBots: { [uid: string]: BotInfo };
  bots: { [uid: string]: Bot & Mover } = {};
  ratings: { [uid: string]: Ratings } = {};
  readonly rateBots: RateBot[] = [];
  readonly uids: { white: string | undefined; black: string | undefined } = {
    white: undefined,
    black: undefined,
  };
  private store: ObjectStorage<BotInfo>;
  private localRatings: ObjectStorage<Ratings>;
  private busy = false;
  private bestMove = { uci: 'e2e4', cp: 30 };

  constructor() {}

  get white(): BotInfo | undefined {
    return this.get(this.uids.white);
  }

  get black(): BotInfo | undefined {
    return this.get(this.uids.black);
  }

  get isBusy(): boolean {
    return this.busy;
  }

  sorted(by: 'alpha' | LocalSpeed = 'alpha'): BotInfo[] {
    if (by === 'alpha') return Object.values(this.bots).sort((a, b) => a.name.localeCompare(b.name));
    else
      return Object.values(this.bots).sort((a, b) => (a.ratings[by]?.r ?? 1500) - (b.ratings[by]?.r ?? 1500));
  }

  async init(serverBots: BotInfo[], assets: Assets): Promise<this> {
    this.assets = assets;
    this.zerofish = await makeZerofish({
      root: site.asset.url('npm', { documentOrigin: true }),
      wasm: site.asset.url('npm/zerofishEngine.wasm'),
      dev: assets.dev,
    });
    if (assets.dev) {
      for (let i = 0; i <= RateBot.MAX_LEVEL; i++) {
        this.rateBots.push(new RateBot(this.zerofish, i));
      }
    }
    return this.initBots(serverBots, assets);
  }

  async initBots(serverBots: BotInfo[], assets: Assets): Promise<this> {
    this.assets = assets;
    await Promise.all([this.resetBots(serverBots), this.assets?.init()]);
    return this;
  }

  async localJson(): Promise<string> {
    return JSON.stringify(await this.store.getMany(), null, 2);
  }

  async clearStoredBots(uids?: string[]): Promise<void> {
    await (uids ? Promise.all(uids.map(uid => this.store.remove(uid))) : this.store.clear());
    return this.resetBots();
  }

  setUid(c: Color, uid: string | undefined): void {
    this.uids[c] = uid;
  }

  setUids({ white, black }: { white?: string | undefined; black?: string | undefined }): void {
    this.uids.white = white;
    this.uids.black = black;
  }

  get(uid: string | undefined): BotInfo | undefined {
    if (uid === undefined) return;
    return this.bots[uid] ?? this.rateBots[Number(uid.slice(1))];
  }

  async move(args: MoveArgs): Promise<MoveResult | undefined> {
    const bot = this[args.chess.turn] as BotInfo & Mover;
    if (!bot) return undefined;
    if (this.busy) return undefined; // just ignore requests from different call stacks
    this.busy = true;
    const score = bot instanceof Bot && bot.needsScore ? (await this.fetchBestMove(args.pos)).cp : undefined;
    const move = await bot?.move({ ...args, score });
    if (!this[co.opposite(args.chess.turn)]) this.bestMove = await this.fetchBestMove(args.pos);
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
          .load(key, this.assets.getSoundUrl(key))
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

  saveBot(bot: Bot): Promise<void> {
    this.bots[bot.uid] = new Bot(bot, this);
    return this.storeBot(bot.uid);
  }

  storeBot(uid: string): Promise<any> {
    if (closeEnough(this.serverBots[uid], this.bots[uid])) return this.store.remove(uid);
    this.localBots[uid] = deepFreeze(structuredClone(this.bots[uid]));
    return this.store.put(uid, this.bots[uid]);
  }

  async deleteBot(uid: string): Promise<void> {
    if (this.uids.white === uid) this.uids.white = undefined;
    if (this.uids.black === uid) this.uids.black = undefined;
    await this.store.remove(uid);
    delete this.bots[uid];
    await this.resetBots();
  }

  updateRatings(speed: LocalSpeed, winner: Color | undefined): Promise<any> {
    const whiteScore = winner === 'white' ? 1 : winner === 'black' ? 0 : 0.5;
    const ratings = [this.white, this.black].map(b => b?.ratings[speed] ?? { r: 1500, rd: 350 });

    return Promise.all([
      this.setRating(this.uids.white, speed, updateGlicko(ratings, whiteScore)),
      this.setRating(this.uids.black, speed, updateGlicko(ratings.reverse(), 1 - whiteScore)),
    ]);

    function updateGlicko(glk: Glicko[], score: number): Glicko {
      const q = Math.log(10) / 400;
      const expected = 1 / (1 + 10 ** ((glk[1].r - glk[0].r) / 400));
      const g = 1 / Math.sqrt(1 + (3 * q ** 2 * glk[1].rd ** 2) / Math.PI ** 2);
      const dSquared = 1 / (q ** 2 * g ** 2 * expected * (1 - expected));
      const deltaR = glk[0].rd <= 0 ? 0 : (q * g * (score - expected)) / (1 / dSquared + 1 / glk[0].rd ** 2);
      return {
        r: Math.round(glk[0].r + deltaR),
        rd: Math.max(30, Math.sqrt(1 / (1 / glk[0].rd ** 2 + 1 / dSquared))),
      };
    }
  }

  imageUrl(bot: BotInfo | undefined): string | undefined {
    return bot?.image && this.assets.getImageUrl(bot.image);
  }

  card(bot: BotInfo | undefined): CardData | undefined {
    return (
      bot && {
        label: bot.name,
        domId: uidToDomId(bot.uid)!,
        imageUrl: this.imageUrl(bot),
        classList: [],
      }
    );
  }

  classifiedCard(bot: BotInfo, isDirty?: (b: BotInfo) => boolean): CardData | undefined {
    const cd = this.card(bot);
    const local = this.localBots[bot.uid];
    const server = this.serverBots[bot.uid];

    if (isDirty?.(local ?? server)) cd?.classList.push('dirty');
    if (server && !closeEnough(server, bot)) {
      if (server.version > bot.version) cd?.classList.push('upstream-changes');
      else if (local && !closeEnough(local, server)) cd?.classList.push('local-changes');
    }
    if (!server) cd?.classList.push('local-only');
    return cd;
  }

  classifiedSort = (a: CardData, b: CardData): number => {
    for (const c of ['dirty', 'local-only', 'local-changes', 'upstream-changes']) {
      if (a.classList.includes(c) && !b.classList.includes(c)) return -1;
      if (!a.classList.includes(c) && b.classList.includes(c)) return 1;
    }
    return 0;
  };

  ratingText(uid: string, speed: LocalSpeed): string {
    const glicko = this.get(uid)?.ratings[speed] ?? { r: 1500, rd: 350 };
    return `${glicko.r}${glicko.rd > 80 ? '?' : ''}`;
  }

  fullRatingText(uid: string, speed: LocalSpeed): string {
    return this.ratingText(uid, speed) + ` (${Math.round(this.get(uid)?.ratings[speed]?.rd ?? 350)})`;
  }

  getRating(uid: string | undefined, speed: LocalSpeed): Glicko {
    if (!uid) return { r: 1500, rd: 350 };
    return this.ratings[uid][speed] ?? { r: 1500, rd: 350 };
  }

  setRating(uid: string | undefined, speed: LocalSpeed, rating: Glicko): Promise<any> {
    if (!uid || !this.bots[uid]) return Promise.resolve();
    // mock ratings until there's a edit UI in place for it, then they'll be pretty much static i think
    // bot.ratings[speed] = rating;
    this.ratings[uid] ??= {};
    this.ratings[uid][speed] = rating;
    if (isEquivalent(this.serverBots[uid]?.ratings, this.bots[uid].ratings))
      return this.localRatings.remove(uid);
    return this.localRatings.put(uid, this.bots[uid].ratings);
  }

  private async resetBots(defBots?: BotInfo[]) {
    const [localBots, serverBots] = await Promise.all([
      this.getStoredBots(),
      defBots ??
        fetch('/local/bots')
          .then(res => res.json())
          .then(res => res.bots),
      this.getStoredRatings(),
    ]);
    for (const b of [...serverBots, ...localBots]) {
      this.bots[b.uid] = new Bot(b, this);
    }
    this.localBots = {};
    this.serverBots = {};

    const freezeIgnoreRatings = (b: BotInfo) =>
      deepFreeze(Object.defineProperty(b, 'ratings', { enumerable: false }));

    localBots.forEach((b: BotInfo) => (this.localBots[b.uid] = freezeIgnoreRatings(b)));
    serverBots.forEach((b: BotInfo) => (this.serverBots[b.uid] = freezeIgnoreRatings(b)));
  }

  private getStoredBots() {
    return (
      this.store?.getMany() ??
      objectStorage<BotInfo>({ store: 'local.bots' }).then(s => {
        this.store = s;
        return s.getMany();
      })
    );
  }

  private async getStoredRatings(): Promise<{ [uid: string]: Ratings }> {
    if (!this.localRatings) this.localRatings = await objectStorage<Ratings>({ store: 'local.bot.ratings' });
    const keys = await this.localRatings.list();
    this.ratings = Object.fromEntries(
      await Promise.all(keys.map(k => this.localRatings.get(k).then(v => [k, v]))),
    );
    return this.ratings;
  }

  private async fetchBestMove(pos: Position): Promise<{ uci: string; cp: number }> {
    const best = (await this.zerofish.goFish(pos, { multipv: 1, by: { depth: 12 } })).lines[0];
    return { uci: best.moves[0], cp: deepScore(best) };
  }
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
