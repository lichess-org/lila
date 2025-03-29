import { type OpeningBook, makeBookFromPolyglot } from 'bits/polyglot';
import { type BotCtrl } from './botCtrl';
import { definedMap } from 'lib/algo';
import { env } from './localEnv';
import { makeLichessBook } from './lichessBook';

export type AssetType = 'image' | 'book' | 'sound' | 'net';

interface NetData {
  key: string;
  data: Uint8Array;
}

export class Assets {
  net: Map<string, Promise<NetData>> = new Map();
  book: Map<string, Promise<OpeningBook>> = new Map();

  constructor(readonly botCtrl?: BotCtrl | undefined) {}

  preload(uids: string[]): Promise<any> {
    const bots = definedMap(uids, uid => (this.botCtrl ?? env.bot).bots.get(uid));
    const books = bots.flatMap(bot => (bot.books ?? []).map(book => book.key));
    const sounds = bots.flatMap(bot =>
      Object.values(bot.sounds ?? {}).flatMap(sounds => sounds.map(sound => sound.key)),
    );
    [...this.book.keys()].filter(k => !books.includes(k)).forEach(release => this.book.delete(release));
    return Promise.all([
      ...books.map(key => fetch(botAssetUrl('book', `${key}.bin`))),
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
}

export function botAssetUrl(type: AssetType, path: string): string {
  return path.startsWith('https:')
    ? path
    : path.includes('/')
      ? `${site.asset.baseUrl()}/assets/${path}`
      : site.asset.url(`lifat/bots/${type}/${encodeURIComponent(path)}`);
}
