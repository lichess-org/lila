import { type OpeningBook, makeBookFromPolyglot } from 'bits/polyglot';
import { type BotCtrl } from './botCtrl';
import { definedMap } from 'common/algo';
import { env } from './localEnv';

export type AssetType = 'image' | 'book' | 'sound' | 'net';

interface NetData {
  key: string;
  data: Uint8Array;
}

export class Assets {
  net: Map<string, Promise<NetData>> = new Map();
  book: Map<string, Promise<OpeningBook>> = new Map();

  constructor(readonly botCtrl?: BotCtrl | undefined) {}

  async preload(uids: string[]): Promise<void> {
    for (const bot of definedMap(uids, uid => (this.botCtrl ?? env.bot).bots.get(uid))) {
      for (const sounds of Object.values(bot.sounds ?? {})) {
        sounds.forEach(sound => fetch(botAssetUrl('sound', sound.key)));
      }
      const books = bot?.books?.flatMap(x => x.key) ?? [];
      [...this.book.keys()].filter(k => !books.includes(k)).forEach(release => this.book.delete(release));
      books.forEach(book => this.getBook(book));
    }
  }

  async getNet(key: string): Promise<Uint8Array> {
    if (this.net.has(key)) return (await this.net.get(key)!).data;
    const netPromise = new Promise<NetData>((resolve, reject) => {
      fetch(botAssetUrl('net', key))
        .then(res => res.arrayBuffer())
        .then(buf => resolve({ key, data: new Uint8Array(buf) }))
        .catch(reject);
    });
    this.net.set(key, netPromise);
    const [lru] = this.net.keys();
    if (this.net.size > 2) this.net.delete(lru);
    return (await netPromise).data;
  }

  async getBook(key: string | undefined): Promise<OpeningBook | undefined> {
    if (!key) return undefined;
    if (this.book.has(key)) return this.book.get(key);
    const bookPromise = new Promise<OpeningBook>((resolve, reject) =>
      fetch(botAssetUrl('book', `${key}.bin`))
        .then(res => res.arrayBuffer())
        .then(buf => makeBookFromPolyglot({ bytes: new DataView(buf) }))
        .then(result => resolve(result.getMoves))
        .catch(reject),
    );
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
