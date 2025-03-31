import makeZerofish, { type Zerofish } from 'zerofish';
import { type OpeningBook, makeBookFromPolyglot } from 'bits/polyglot';
import { Bot } from './bot';
import { defined } from 'lib';
import { pubsub } from 'lib/pubsub';
import type { BotInfo, SoundEvent, MoveSource, MoveArgs, MoveResult, LocalSpeed, AssetType } from './types';
import * as xhr from 'lib/xhr';
import { definedMap } from 'lib/algo';
import { makeLichessBook } from './lichessBook';

export class BotCtrl {
  zerofish: Zerofish;
  readonly net: Map<string, Promise<NetData>> = new Map();
  readonly book: Map<string, Promise<OpeningBook>> = new Map();
  readonly bots: Map<string, Bot & MoveSource> = new Map();
  readonly uids: Record<Color, string | undefined> = { white: undefined, black: undefined };
  protected busy = false;

  constructor(zf?: Zerofish | false) {
    // pass nothing for normal behavior, custom instance, or false to stub
    if (zf) this.zerofish = zf;
    else if (zf === false)
      this.zerofish = {
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
    const [bots] = await Promise.all([
      defBots ?? xhr.json('/bots').then(res => res.bots),
      this.zerofish ??
        makeZerofish({
          locator: (file: string) => site.asset.url(`npm/${file}`, { documentOrigin: file.endsWith('js') }),
          nonce: document.body.dataset.nonce,
        }).then(zf => (this.zerofish = zf)),
    ]);
    for (const b of [...bots]) {
      this.bots.set(b.uid, new Bot(b, this));
    }
    if (this.uids.white && !this.bots.has(this.uids.white)) this.uids.white = undefined;
    if (this.uids.black && !this.bots.has(this.uids.black)) this.uids.black = undefined;
    this.reset();
    pubsub.complete('local.bots.ready');
    return this;
  }

  async move(args: MoveArgs): Promise<MoveResult | undefined> {
    const bot = this[args.chess.turn] as BotInfo & MoveSource;
    if (!bot) return undefined;
    if (this.busy) return undefined; // ignore different call stacks
    this.busy = true;
    const move = await bot?.move(args);
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
    this.reset();
    this.preload();
  }

  reset(): void {
    return this.zerofish?.reset();
  }

  imageUrl(bot: BotInfo | undefined): string | undefined {
    return bot?.image && this.getImageUrl(bot.image);
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
          .load(key, this.getSoundUrl(key))
          .then(() => setTimeout(() => site.sound.play(key, Math.min(1, mix * 2)), delay * 1000));
        return Math.min(1, (1 - mix) * 2);
      }
    }
    return 1;
  }

  preload(uids: string[] = [this.uids.white, this.uids.black].filter(defined)): Promise<any> {
    const bots = definedMap(uids, uid => this.bots.get(uid));
    const books = bots.flatMap(bot => (bot.books ?? []).map(book => book.key));
    const nets = bots.flatMap(bot => (bot.zero?.net ? [bot.zero.net] : []));
    const sounds = [
      ...new Set(
        bots.flatMap(bot =>
          Object.values(bot.sounds ?? {}).flatMap(sounds => sounds.map(sound => sound.key)),
        ),
      ),
    ];
    [...this.book.keys()]
      .filter(k => k !== 'lichess' && !books.includes(k))
      .forEach(garbageCollect => this.book.delete(garbageCollect));
    return Promise.all([
      ...nets.map(key => this.getNet(key)),
      ...books.map(key => this.getBook(key)),
      ...sounds.map(key => fetch(botAssetUrl('sound', key))),
    ]);
  }

  getNet(key: string): Promise<Uint8Array> {
    if (this.net.has(key)) return this.net.get(key)!.then(net => net.data);
    const netPromise = fetch(botAssetUrl('net', key))
      .then(res => res.arrayBuffer())
      .then(buf => ({ key, data: new Uint8Array(buf) }));
    this.net.set(key, netPromise);
    const [lru] = this.net.keys();
    if (this.net.size > 2) this.net.delete(lru);
    return netPromise.then(net => net.data);
  }

  getBook(key: string | undefined): Promise<OpeningBook | undefined> {
    if (!key) return Promise.resolve(undefined);
    if (this.book.has(key)) return Promise.resolve(this.book.get(key));
    const bookPromise =
      key === 'lichess'
        ? Promise.resolve(makeLichessBook())
        : fetch(botAssetUrl('book', `${key}.bin`))
            .then(res => res.arrayBuffer())
            .then(buf => makeBookFromPolyglot({ bytes: new DataView(buf) }))
            .then(result => result.getMoves);
    this.book.set(key, bookPromise);
    return bookPromise;
  }

  getImageUrl(key: string): string {
    return botAssetUrl('image', key);
  }

  getSoundUrl(key: string): string {
    return botAssetUrl('sound', key);
  }

  protected storedBots(): Promise<BotInfo[]> {
    return Promise.resolve([]);
  }
}

export function botAssetUrl(type: AssetType, path: string): string {
  return path.startsWith('https:')
    ? path
    : path.includes('/')
      ? `${site.asset.baseUrl()}/assets/${path}`
      : site.asset.url(`lifat/bots/${type}/${encodeURIComponent(path)}`);
}

interface NetData {
  key: string;
  data: Uint8Array;
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
