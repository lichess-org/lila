import { type Zerofish, type Position } from 'zerofish';
import { Bot, score } from './bot';
import { defined } from 'common';
import { deepFreeze } from 'common/algo';
import { pubsub } from 'common/pubsub';
import type { BotInfo, SoundEvent, MoveSource, MoveArgs, MoveResult, LocalSpeed } from './types';
import { env } from './localEnv';
import * as xhr from 'common/xhr';
import { Assets } from './assets';

export class BotCtrl {
  zerofish: Zerofish;
  localBots: Record<string, BotInfo> = {};
  serverBots: Record<string, BotInfo>;
  readonly bots: Map<string, Bot & MoveSource> = new Map();
  readonly uids: Record<Color, string | undefined> = { white: undefined, black: undefined };
  protected busy = false;

  constructor(zf?: Zerofish) {
    this.zerofish = zf ?? {
      goZero: () => Promise.resolve({ lines: [], bestmove: '', engine: 'zero' }),
      goFish: () => Promise.resolve({ lines: [], bestmove: '', engine: 'fish' }),
      quit: () => {},
      stop: () => {},
      reset: () => {},
    };
  }

  get white(): BotInfo | undefined {
    return this.get(this.uids.white);
  }

  get black(): BotInfo | undefined {
    return this.get(this.uids.black);
  }

  get isBusy(): boolean {
    return this.busy;
  }

  get all(): BotInfo[] {
    return [...this.bots.values()] as Bot[];
  }

  get playing(): BotInfo[] {
    return [this.white, this.black].filter(defined);
  }

  async init(defBots?: BotInfo[]): Promise<this> {
    const [localBots, serverBots] = await Promise.all([
      this.storedBots(),
      defBots?.filter(Bot.viable) ?? xhr.json('/bots').then(res => res.bots.filter(Bot.viable)),
    ]);
    for (const b of [...serverBots, ...localBots]) {
      if (Bot.viable(b)) this.bots.set(b.uid, new Bot(b));
    }
    this.serverBots = {};
    localBots.forEach((b: BotInfo) => (this.localBots[b.uid] = deepFreeze(b)));
    serverBots.forEach((b: BotInfo) => (this.serverBots[b.uid] = deepFreeze(b)));
    if (this.uids.white && !this.bots.has(this.uids.white)) this.uids.white = undefined;
    if (this.uids.black && !this.bots.has(this.uids.black)) this.uids.black = undefined;
    pubsub.complete('local.bots.ready');
    return this;
  }

  async move(args: MoveArgs): Promise<MoveResult | undefined> {
    const bot = this[args.chess.turn] as BotInfo & MoveSource;
    if (!bot) return undefined;
    if (this.busy) return undefined; // ignore different call stacks
    this.busy = true;
    const cp = bot instanceof Bot && bot.needsScore ? (await this.fetchBestMove(args.pos)).cp : undefined;
    const move = await bot?.move({ ...args, cp });
    this.busy = false;
    return move?.uci !== '0000' ? move : undefined;
  }

  get(uid: string | undefined): BotInfo | undefined {
    if (uid === undefined) return undefined;
    return this.bots.get(uid);
  }

  sorted(by: 'alpha' | LocalSpeed = 'alpha'): BotInfo[] {
    return [...this.bots.values()].sort((a, b) => {
      return (by !== 'alpha' && Bot.rating(a, by) - Bot.rating(b, by)) || a.name.localeCompare(b.name);
    });
  }

  setUids({ white, black }: { white?: string | undefined; black?: string | undefined }): void {
    this.uids.white = white;
    this.uids.black = black;
  }

  reset(): void {
    return this.zerofish?.reset();
  }

  imageUrl(bot: BotInfo | undefined): string | undefined {
    return bot?.image && env.assets.getImageUrl(bot.image);
  }

  playSound(c: Color, eventList: SoundEvent[], assets?: Assets): number {
    const prioritized = soundPriority.filter(e => eventList.includes(e));
    for (const soundList of prioritized.map(priority => this[c]?.sounds?.[priority] ?? [])) {
      let r = Math.random();
      for (const { key, chance, delay, mix } of soundList) {
        r -= chance / 100;
        if (r > 0) continue;
        // right now we play at most one sound per move, might want to revisit this.
        // also definitely need cancelation of the timeout
        site.sound
          .load(key, (assets || env.assets).getSoundUrl(key))
          .then(() => setTimeout(() => site.sound.play(key, Math.min(1, mix * 2)), delay * 1000));
        return Math.min(1, (1 - mix) * 2);
      }
    }
    return 1;
  }

  protected storedBots(): Promise<BotInfo[]> {
    return Promise.resolve([]);
  }

  protected async fetchBestMove(pos: Position): Promise<{ uci: string; cp: number }> {
    const best = (await this.zerofish.goFish(pos, { multipv: 1, by: { depth: 12 } })).lines[0];
    return { uci: best.moves[0], cp: score(best) };
  }
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
