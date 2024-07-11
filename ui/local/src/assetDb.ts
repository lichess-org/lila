import { type ObjectStorage, objectStorage } from 'common/objectStorage';
import { setSchemaAssets } from './dev/schema';
import type { NetData } from './types';
import type { PolyglotBook } from 'bits/types';

export type AssetType = 'net' | 'book' | 'image' | 'sound';
export class AssetDb {
  private db = {
    net: new Store('nets'),
    book: new Store('books'),
    image: new Store('images'),
    sound: new Store('sounds'),
  };
  private cached: {
    net: Map<string, NetData>;
    book: Map<string, PolyglotBook>;
    image: Map<string, string>;
    sound: Map<string, string>;
  };

  ready = new Promise<void>(resolve => this.init().then(() => resolve()));

  constructor(readonly remote?: { net: string[]; image: string[]; book: string[]; sound: string[] }) {}

  async init() {
    const then = Date.now();
    this.cached = { net: new Map(), book: new Map(), image: new Map(), sound: new Map() };
    await Promise.allSettled(Object.values(this.db).map(s => s.init()));
    const imageKeys = await this.db.image.list();
    const images = (await Promise.allSettled(imageKeys.map(k => this.db.image.get(k)) ?? [])).map(
      r => r.status === 'fulfilled' && r.value,
    );
    for (let i = 0; i < imageKeys.length; i++) {
      const [key, data] = [imageKeys[i], images[i]];
      const o = { type: extToMime(key) };
      if (data) this.cached.image.set(key, URL.createObjectURL(new Blob([data], o)));
    }
    if (this.remote) await this.setBotEditorAssets();
    console.log('AssetDb init', Date.now() - then, 'ms');
    return this;
  }

  local = (type: AssetType) => this.db[type].list();

  add = async (type: AssetType, key: string, file: Blob) => {
    this.db[type].store?.put(key, new Uint8Array(await arrayBuffer(file)));
    if (type === 'image' || type === 'sound') {
      const oldUrl = this.cached[type].get(key);
      if (oldUrl) URL.revokeObjectURL(oldUrl);
      this.cached[type].set(key, URL.createObjectURL(file));
    }
    this.setBotEditorAssets(); // TODO, update dialog somehow
  };

  delete = async (type: AssetType, key: string) => {
    await this.db[type].store?.remove(key);
    if (type === 'image' || type === 'sound') {
      const oldUrl = this.cached[type].get(key);
      if (oldUrl) URL.revokeObjectURL(oldUrl);
      this.cached[type].delete(key);
    }
    this.setBotEditorAssets(); // TODO, update dialog somehow
  };

  getNet = async (key: string | undefined): Promise<Uint8Array | undefined> => {
    if (!key) return undefined;
    await this.ready;
    const cached = this.cached.net.get(key);
    if (cached) return cached.data;
    const data = await this.db.net.get(key);
    this.cached.net.set(key, { key, data });
    if (this.cached.net.size > 2) this.cached.net.delete(this.cached.net.keys().next().value);
    return data;
  };

  getBook = async (key: string | undefined): Promise<PolyglotBook | undefined> => {
    if (!key) return undefined;
    await this.ready;
    const cached = this.cached.book.get(key);
    if (cached) return cached;
    const book = await this.db.book
      .get(key)
      .then(arr => site.asset.loadEsm<PolyglotBook>('bits.polyglot', { init: new DataView(arr.buffer) }));
    this.cached.book.set(key, book);
    return book;
  };

  getImageUrl = (key: string) => this.cached.image.get(key) ?? botAssetUrl(`images/${key}`);

  getSoundUrl = (key: string) => this.cached.sound.get(key) ?? botAssetUrl(`sounds/${key}`);

  private async setBotEditorAssets() {
    if (!this.remote) return;
    const [idbNets, idbBooks, idbImages] = await Promise.all(
      [this.db.net, this.db.book, this.db.image].map(s => s.list()),
    );
    const combined = {
      nets: [...new Set([...this.remote.net, ...idbNets])],
      books: [...new Set([...this.remote.book, ...idbBooks])],
      images: [...new Set([...this.remote.image, ...idbImages])],
    };
    setSchemaAssets(combined);
  }
}

export function botAssetUrl(name: string, version: string | false = 'bot000') {
  return site.asset.url(`lifat/bots/${name}`, { version });
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
  store: ObjectStorage<Uint8Array, string>;
  constructor(readonly type: string) {}
  async init() {
    this.store = await objectStorage<Uint8Array, string>({ store: `local.${this.type}` });
    return this;
  }
  async get(key: string): Promise<Uint8Array> {
    const data =
      (await this.store?.get(key)) ??
      (await fetch(botAssetUrl(`${this.type}/${key}`, false))
        .then(res => res.arrayBuffer())
        .then(buf => new Uint8Array(buf)));
    if (!data) throw new Error(`error fetching ${this.type}/${key}`);
    this.store?.put(key, data);
    return data;
  }
  async list() {
    return this.store?.list() ?? [];
  }
}
