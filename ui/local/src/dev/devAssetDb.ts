import { type ObjectStorage, objectStorage } from 'common/objectStorage';
import type { NetData } from '../types';
import { botAssetUrl, AssetDb } from '../assetDb';
import { makeBookFromPolyglot, type OpeningBook, type PolyglotResult } from 'bits/polyglot';

export type AssetType = 'book' | 'bookCover' | 'image' | 'sound' | 'net';
export type AssetList = {
  net: string[];
  image: string[];
  book: string[];
  sound: string[];
  bookCover?: string[];
};

export class DevAssetDb extends AssetDb {
  private db = {
    net: new Store('net'),
    book: new Store('book'),
    bookCover: new Store('bookCover'),
    image: new Store('image'),
    sound: new Store('sound'),
  };
  bookCover: Map<string, string> = new Map();
  image: Map<string, string> = new Map();
  sound: Map<string, string> = new Map();

  constructor(public remote: AssetList) {
    super();
    remote.bookCover = [];
    remote.book.forEach(book => remote.bookCover!.push(`${book}.png`));
  }

  get dev() {
    return true;
  }

  async init(): Promise<this> {
    const then = Date.now();
    await Promise.allSettled(Object.values(this.db).map(s => s.init()));
    const [localImages, localSounds, localBookCovers] = await Promise.all([
      this.db.image.getAll(),
      this.db.sound.getAll(),
      this.db.bookCover.getAll(),
      this.db.book.updateKeys(),
      ...(this.remote?.image.map(async key => [key, await fetch(botAssetUrl(`images/${key}`, false))]) ?? []),
    ]);
    for (const [key, data] of localSounds) {
      this.sound.set(key, URL.createObjectURL(new Blob([data], { type: 'audio/mpeg' })));
    }
    for (const [key, data] of localImages) {
      this.image.set(key, URL.createObjectURL(new Blob([data], { type: extToMime(key) })));
    }
    for (const [key, data] of localBookCovers) {
      this.bookCover.set(key, URL.createObjectURL(new Blob([data], { type: extToMime(key) })));
    }
    console.log('DevAssetDb init', Date.now() - then, 'ms');
    return this;
  }

  update(remote: AssetList): void {
    this.remote = remote;
    remote.bookCover = [];
    remote.book.forEach(book => remote.bookCover!.push(`${book}.png`));
  }

  local(type: AssetType): string[] {
    if (type === 'net') return [];
    return this.db[type].keys.filter(k => !this.remote?.[type]?.includes(k));
  }

  all(type: AssetType): string[] {
    return [...new Set([...(this.remote?.[type] ?? []), ...this.db[type].keys])];
  }

  blob(type: AssetType, key: string): Promise<Blob> {
    return this.db[type].get(key);
  }

  async add(type: AssetType, key: string, file: Blob): Promise<any> {
    if (type === 'book') return this.addBook(key, file);
    await this.db[type].store?.put(key, file);
    if (type === 'image' || type === 'sound') {
      const oldUrl = this[type].get(key);
      if (oldUrl) URL.revokeObjectURL(oldUrl);
      this[type].set(key, URL.createObjectURL(file));
    }
    await this.db[type].updateKeys();
  }

  async addBook(key: string, file: Blob): Promise<string> {
    const data = await arrayBuffer(file);
    const book = await makeBookFromPolyglot(new DataView(data), { depth: 2, boardSize: 192 });
    if (!book.cover) throw new Error(`error parsing ${key}`);
    if (key.endsWith('.bin')) key = key.slice(0, -4);
    await Promise.all([
      this.db.book.store.put(key, file),
      this.db.bookCover.store.put(`${key}.png`, book.cover),
    ]);
    this.book.set(key, book.getMoves);
    this.bookCover.set(key, URL.createObjectURL(new Blob([book.cover], { type: 'image/png' })));
    await this.db.book.updateKeys();
    return key;
  }

  async delete(type: AssetType, key: string): Promise<void> {
    await Promise.all(
      type === 'book'
        ? [this.db.book.store?.remove(key), this.db.bookCover.store?.remove(key)]
        : [this.db[type].store?.remove(key)],
    );
    if (type === 'image' || type === 'sound') {
      const oldUrl = this[type].get(key);
      if (oldUrl) URL.revokeObjectURL(oldUrl);
      this[type].delete(key);
    }
    await this.db[type].updateKeys();
  }

  async getBook(key: string | undefined): Promise<OpeningBook | undefined> {
    if (!key) return undefined;
    const cached = this.book.get(key);
    if (cached) return cached;
    const bookBlob = await (this.db.book.keys.includes(key)
      ? this.db.book.get(key)
      : fetch(botAssetUrl(`books/${key}.bin`)).then(res => res.blob()));
    const bytes = new DataView(await bookBlob.arrayBuffer());
    const book = await makeBookFromPolyglot(bytes);
    this.book.set(key, book.getMoves);
    return book.getMoves;
  }

  getImageUrl(key: string): string {
    return this.image.get(key) ?? botAssetUrl(`images/${key}`);
  }

  getSoundUrl(key: string): string {
    return this.sound.get(key) ?? botAssetUrl(`sounds/${key}`);
  }

  getBookCoverUrl(key: string): string {
    return this.bookCover.get(key) ?? botAssetUrl(`books/${key}.png`);
  }
}

function arrayBuffer(file: Blob): Promise<ArrayBuffer> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as ArrayBuffer);
    reader.onerror = reject;
    reader.readAsArrayBuffer(file);
  });
}

function extToMime(filename: string) {
  switch (filename.slice(filename.lastIndexOf('.') + 1).toLowerCase()) {
    case 'png':
      return 'image/png';
    case 'jpg':
    case 'jpeg':
      return 'image/jpeg';
    case 'gif':
      return 'image/gif';
    case 'svg':
      return 'image/svg+xml';
    case 'webp':
      return 'image/webp';
    case 'mp3':
      return 'audio/mpeg';
    default:
      return 'application/octet-stream';
  }
}

class Store {
  store: ObjectStorage<Blob, string>;
  keys: string[] = [];

  constructor(readonly type: AssetType) {}

  async init() {
    this.store = await objectStorage<Blob, string>({ store: `local.${this.type}s` });
    return this;
  }

  async getAll(): Promise<[string, Blob][]> {
    const list = await this.updateKeys();
    return Promise.all(list.map(async k => [k, await this.store.get(k)]));
  }

  async get(key: string): Promise<Blob> {
    const data = await this.store?.get(key);
    if (!data) throw new Error(`error fetching ${this.type}s/${key}`);
    return data;
  }

  async updateKeys() {
    this.keys = await this.store?.list();
    Object.freeze(this.keys);
    return this.keys;
  }
}
