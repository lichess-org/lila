import makeZerofish, { type Zerofish, type Position } from 'zerofish';
import * as co from 'chessops';
import { Bot, score } from './bot';
import { RateBot } from './dev/rateBot';
import { closeEnough } from './dev/devUtil';
import { type CardData } from './handOfCards';
import { type ObjectStorage, objectStorage } from 'common/objectStorage';
import { defined } from 'common';
import { deepFreeze } from 'common/algo';
import { pubsub } from 'common/pubsub';
import type { BotInfo, SoundEvent, MoveSource, MoveArgs, MoveResult, LocalSpeed } from './types';
import { env } from './localEnv';

export class BotCtrl {
  zerofish: Zerofish;
  serverBots: Record<string, BotInfo>;
  localBots: Record<string, BotInfo>;
  readonly bots: Record<string, Bot & MoveSource> = {};
  readonly rateBots: RateBot[] = [];
  readonly uids: Record<Color, string | undefined> = { white: undefined, black: undefined };
  private store: ObjectStorage<BotInfo>;
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

  get firstUid(): string | undefined {
    return Object.keys(this.bots)[0];
  }

  get all(): BotInfo[] {
    // except for rate bots
    return Object.values(this.bots) as Bot[];
  }

  get playing(): BotInfo[] {
    return [this.white, this.black].filter(defined);
  }

  async init(serverBots: BotInfo[]): Promise<this> {
    this.zerofish = await makeZerofish({
      root: site.asset.url('npm', { documentOrigin: true, version: false }),
      wasm: site.asset.url('npm/zerofishEngine.wasm', { version: false }),
      dev: env.isDevPage,
    });
    if (env.isDevPage) {
      for (let i = 0; i <= RateBot.MAX_LEVEL; i++) {
        this.rateBots.push(new RateBot(i));
      }
      //site.pubsub.on('local.dev.import.book', this.onBookImported);
    }
    return this.initBots(serverBots!);
  }

  async initBots(defBots?: BotInfo[]): Promise<this> {
    const [localBots, serverBots] = await Promise.all([
      this.getSavedBots(),
      defBots ??
        fetch('/local/bots')
          .then(res => res.json())
          .then(res => res.bots),
    ]);
    for (const b of [...serverBots, ...localBots]) {
      if (Bot.viable(b)) this.bots[b.uid] = new Bot(b);
    }
    this.localBots = {};
    this.serverBots = {};
    localBots.forEach((b: BotInfo) => (this.localBots[b.uid] = deepFreeze(b)));
    serverBots.forEach((b: BotInfo) => (this.serverBots[b.uid] = deepFreeze(b)));
    pubsub.complete('local.bots.ready');
    return this;
  }

  async move(args: MoveArgs): Promise<MoveResult | undefined> {
    const bot = this[args.chess.turn] as BotInfo & MoveSource;
    if (!bot) return undefined;
    if (this.busy) return undefined; // just ignore requests from different call stacks
    this.busy = true;
    const cp = bot instanceof Bot && bot.needsScore ? (await this.fetchBestMove(args.pos)).cp : undefined;
    const move = await bot?.move({ ...args, cp });
    if (!this[co.opposite(args.chess.turn)]) this.bestMove = await this.fetchBestMove(args.pos);
    this.busy = false;
    return move?.uci !== '0000' ? move : undefined;
  }

  get(uid: string | undefined): BotInfo | undefined {
    if (uid === undefined) return;
    return this.bots[uid] ?? this.rateBots[Number(uid.slice(1))];
  }

  sorted(by: 'alpha' | LocalSpeed = 'alpha'): BotInfo[] {
    if (by === 'alpha') return Object.values(this.bots).sort((a, b) => a.name.localeCompare(b.name));
    else
      return Object.values(this.bots).sort((a, b) => {
        return (a.ratings[by] ?? 1500) - (b.ratings[by] ?? 1500) || a.name.localeCompare(b.name);
      });
  }

  setUids({ white, black }: { white?: string | undefined; black?: string | undefined }): void {
    this.uids.white = white;
    this.uids.black = black;
  }

  stop(): void {
    return this.zerofish.stop();
  }

  reset(): void {
    this.bestMove = { uci: 'e2e4', cp: 30 };
    return this.zerofish.reset();
  }

  save(bot: BotInfo): Promise<any> {
    delete this.localBots[bot.uid];
    this.bots[bot.uid] = new Bot(bot);
    if (closeEnough(this.serverBots[bot.uid], bot)) return this.store.remove(bot.uid);
    this.localBots[bot.uid] = deepFreeze(structuredClone(bot));
    return this.store.put(bot.uid, bot);
  }

  async setServer(bot: BotInfo): Promise<void> {
    this.bots[bot.uid] = new Bot(bot);
    this.serverBots[bot.uid] = deepFreeze(structuredClone(bot));
    delete this.localBots[bot.uid];
    await this.store.remove(bot.uid);
  }

  async delete(uid: string): Promise<void> {
    if (this.uids.white === uid) this.uids.white = undefined;
    if (this.uids.black === uid) this.uids.black = undefined;
    await this.store.remove(uid);
    delete this.bots[uid];
    await this.initBots();
  }

  imageUrl(bot: BotInfo | undefined): string | undefined {
    return bot?.image && env.assets.getImageUrl(bot.image);
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
    if (!server) cd?.classList.push('local-only');
    else if (server.version > bot.version) cd?.classList.push('upstream-changes');
    else if (local && !closeEnough(local, server)) cd?.classList.push('local-changes');
    return cd;
  }

  classifiedSort(speed: LocalSpeed = 'classical'): (a: CardData, b: CardData) => number {
    return (a, b) => {
      for (const c of ['dirty', 'local-only', 'local-changes', 'upstream-changes']) {
        if (a.classList.includes(c) && !b.classList.includes(c)) return -1;
        if (!a.classList.includes(c) && b.classList.includes(c)) return 1;
      }
      const [ab, bb] = [this.get(domIdToUid(a.domId)), this.get(domIdToUid(b.domId))];
      return (ab?.ratings[speed] ?? 1500) - (bb?.ratings[speed] ?? 1500) || a.label.localeCompare(b.label);
    };
  }

  async clearStoredBots(uids?: string[]): Promise<void> {
    await (uids ? Promise.all(uids.map(uid => this.store.remove(uid))) : this.store.clear());
    await this.initBots();
  }

  playSound(c: Color, eventList: SoundEvent[]): number {
    const prioritized = soundPriority.filter(e => eventList.includes(e));
    for (const soundList of prioritized.map(priority => this[c]?.sounds?.[priority] ?? [])) {
      let r = Math.random();
      for (const { key, chance, delay, mix } of soundList) {
        r -= chance / 100;
        if (r > 0) continue;
        // right now we play at most one sound per move, might want to revisit this.
        // also definitely need cancelation of the timeout
        site.sound
          .load(key, env.assets.getSoundUrl(key))
          .then(() => setTimeout(() => site.sound.play(key, Math.min(1, mix * 2)), delay * 1000));
        return Math.min(1, (1 - mix) * 2);
      }
    }
    return 1;
  }

  // private onBookImported = (key: string, oldKey?: string): void => {
  //   if (!oldKey) return;

  // }

  private getSavedBots() {
    return (
      this.store?.getMany() ??
      objectStorage<BotInfo>({ store: 'local.bots' }).then(s => {
        this.store = s;
        return s.getMany();
      })
    );
  }

  private async fetchBestMove(pos: Position): Promise<{ uci: string; cp: number }> {
    const best = (await this.zerofish.goFish(pos, { multipv: 1, by: { depth: 12 } })).lines[0];
    return { uci: best.moves[0], cp: score(best) };
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
