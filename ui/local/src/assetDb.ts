import type { NetData } from './types';
import { type OpeningBook, makeBookFromPolyglot } from 'bits/polyglot';

export class AssetDb {
  net: Map<string, NetData> = new Map();
  book: Map<string, OpeningBook> = new Map();

  get dev(): boolean {
    return false;
  }

  async init(): Promise<this> {
    // prefetch images here
    return this;
  }

  async getNet(key: string | undefined): Promise<Uint8Array | undefined> {
    if (!key) return undefined;
    const cached = this.net.get(key);
    if (cached) return cached.data;
    const data = await fetch(botAssetUrl(`nets/${key}`, false))
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
    const buf = await fetch(botAssetUrl(`books/${key}.bin`)).then(res => res.arrayBuffer());
    const book = (await makeBookFromPolyglot(new DataView(buf))).getMoves;
    this.book.set(key, book);
    return book;
  }

  getImageUrl(key: string): string {
    return botAssetUrl(`images/${key}`);
  }

  getSoundUrl(key: string): string {
    return botAssetUrl(`sounds/${key}`);
  }
}

export function botAssetUrl(name: string, version: string | false = 'bot000'): string {
  return site.asset.url(`lifat/bots/${name}`, { version });
}
