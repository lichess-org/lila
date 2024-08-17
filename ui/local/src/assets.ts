import { type OpeningBook, makeBookFromPolyglot } from 'bits/polyglot';

export type AssetType = 'image' | 'book' | 'sound' | 'net';

export class Assets {
  net: Map<string, NetData> = new Map();
  book: Map<string, OpeningBook> = new Map();

  async init(): Promise<this> {
    // prefetch stuff here or in service worker install \o/
    return this;
  }

  async getNet(key: string | undefined): Promise<Uint8Array | undefined> {
    if (!key) return undefined;
    const cached = this.net.get(key);
    if (cached) return cached.data;
    const data = await fetch(botAssetUrl('net', key, false))
      .then(res => res.arrayBuffer())
      .then(buf => new Uint8Array(buf));
    this.net.set(key, { key, data });
    if (this.net.size > 2) this.net.delete(this.net.keys().next().value);
    return data;
  }

  async getBook(key: string | undefined): Promise<OpeningBook | undefined> {
    if (!key) return undefined;
    const cached = this.book.get(key);
    if (cached) return cached;
    const buf = await fetch(botAssetUrl('book', `${key}.bin`, false)).then(res => res.arrayBuffer());
    const book = (await makeBookFromPolyglot(new DataView(buf))).getMoves;
    this.book.set(key, book);
    return book;
  }

  getImageUrl(key: string): string {
    return botAssetUrl('image', key);
  }

  getSoundUrl(key: string): string {
    return botAssetUrl('sound', key);
  }
}

export function botAssetUrl(type: AssetType, name: string, version: string | false = 'bot000'): string {
  return site.asset.url(`lifat/bots/${type}s/${encodeURIComponent(name)}`, { version });
}

type NetData = {
  key: string;
  data: Uint8Array;
};
