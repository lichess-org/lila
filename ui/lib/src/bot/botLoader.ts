import makeZerofish, { type Zerofish } from 'zerofish';
import { type OpeningBook, makeBookFromPolyglot } from '../game/polyglot';
import { Bot } from './bot';
import type { BotInfo, MoveSource, LocalSpeed, AssetType } from './types';
import * as xhr from '../xhr';
import { definedMap } from '../algo';
import { makeLichessBook } from './lichessBook';
import { myUserId, myUsername } from '../common';

export class BotLoader {
  zerofish: Zerofish;
  readonly net: Map<string, Promise<{ key: string; data: Uint8Array }>> = new Map();
  readonly book: Map<string, Promise<OpeningBook>> = new Map();
  readonly bots: Map<string, Bot & MoveSource> = new Map();
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

  async init(defBots?: BotInfo[]): Promise<this> {
    const [bots] = await Promise.all([
      defBots ?? xhr.json('/bots').then(res => res.bots),
      this.zerofish ??
        makeZerofish({
          locator: (file: string) => site.asset.url(`npm/${file}`, { documentOrigin: file.endsWith('js') }),
        }).then(zf => (this.zerofish = zf)),
    ]);
    for (const b of [...bots].filter(Bot.isValid)) {
      this.bots.set(b.uid, new Bot(b, this));
    }
    this.reset();
    return this;
  }

  sorted(by: 'alpha' | LocalSpeed = 'alpha'): BotInfo[] {
    return [...this.bots.values()].sort((a, b) => {
      return (by !== 'alpha' && Bot.rating(a, by) - Bot.rating(b, by)) || a.name.localeCompare(b.name);
    });
  }

  reset(): void {
    return this.zerofish?.reset();
  }

  imageUrl(bot: BotInfo | undefined): string | undefined {
    return bot?.image && this.getImageUrl(bot.image);
  }

  preload(uids: string[] | string): Promise<any> {
    if (!Array.isArray(uids)) uids = [uids];
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
      .forEach(free => this.book.delete(free));
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

  nameOf(uid?: string): string {
    return !uid || uid === myUserId()
      ? (myUsername() ?? 'Anonymous')
      : (uid.startsWith('#') && this.bots.get(uid)?.name) || uid.charAt(0).toUpperCase() + uid.slice(1);
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
