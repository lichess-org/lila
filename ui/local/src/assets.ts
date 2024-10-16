import { type OpeningBook, makeBookFromPolyglot } from 'bits/polyglot';
import { defined } from 'common';
import { pubsub } from 'common/pubsub';
import { env } from './localEnv';

export type AssetType = 'image' | 'book' | 'sound' | 'net';

export class Assets {
  net: Map<string, Promise<NetData>> = new Map();
  book: Map<string, Promise<OpeningBook>> = new Map();

  async init(): Promise<this> {
    // prefetch stuff here or in service worker install \o/
    await pubsub.after('local.bots.ready');
    await Promise.all(
      [...new Set<string>(Object.values(env.bot.bots).map(b => this.getImageUrl(b.image)))].map(
        url =>
          new Promise<void>(resolve => {
            const img = new Image();
            img.src = url;
            img.onload = () => resolve();
            img.onerror = () => resolve();
          }),
      ),
    );
    pubsub.complete('local.images.ready');
    return this;
  }

  async preload(): Promise<void> {
    for (const bot of [env.bot.white, env.bot.black].filter(defined)) {
      for (const sounds of Object.values(bot.sounds ?? {})) {
        sounds.forEach(sound => fetch(botAssetUrl('sound', sound.key)));
      }
    }
    const books = (['white', 'black'] as const).flatMap(c => env.bot[c]?.books?.flatMap(x => x.key) ?? []);
    [...this.book.keys()].filter(k => !books.includes(k)).forEach(release => this.book.delete(release));
    books.forEach(book => this.getBook(book));
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

export function botAssetUrl(type: AssetType, name: string): string {
  return site.asset.url(`lifat/bots/${type}/${encodeURIComponent(name)}`, { version: false });
}

type NetData = {
  key: string;
  data: Uint8Array;
};
