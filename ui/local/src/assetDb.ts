import { type ObjectStorage, objectStorage } from 'common/objectStorage';
import { setSchemaAssets } from './dev/schema';
import type { NetData } from './types';
import type { PolyglotBook } from 'bits/types';

export class AssetDb {
  private db = { nets: new Store('nets'), books: new Store('books'), images: new Store('images') };
  private cached: {
    nets: NetData[];
    books: { [key: string]: PolyglotBook };
    images: { [key: string]: string };
  };

  ready = new Promise<void>(resolve => this.init().then(() => resolve()));

  constructor(readonly assets?: { nets: string[]; images: string[]; books: string[] }) {}

  async init() {
    const then = Date.now();
    this.cached = { nets: [], books: {}, images: {} };
    await Promise.all(Object.values(this.db).map(s => s.init()));
    const imageKeys = (await this.db.images.store?.list()) ?? [];
    const images = (await Promise.allSettled(imageKeys?.map(k => this.db.images.get(k)) ?? [])).map(
      r => r.status === 'fulfilled' && r.value,
    );
    for (let i = 0; i < imageKeys.length; i++) {
      const [key, data] = [imageKeys[i], images[i]];
      const o = { type: extToMime(key) };
      if (data) this.cached.images[key] = URL.createObjectURL(new Blob([data], o));
    }
    if (this.assets) await this.setBotEditorAssets();
    console.trace('AssetDb init', Date.now() - then, 'ms');
    return this;
  }

  listNets = () => this.assets?.nets ?? [];
  listBooks = () => this.assets?.books ?? [];

  addNet = async (key: string, file: File) => {
    this.db.nets.store?.put(key, new Uint8Array(await arrayBuffer(file)));
    this.setBotEditorAssets();
  };

  addBook = async (key: string, file: File) => {
    this.db.books.store?.put(key, new Uint8Array(await arrayBuffer(file)));
    this.setBotEditorAssets();
  };

  addImage = async (key: string, file: File) => {
    if (!file.type.startsWith('image/')) return;
    await this.db.images.store?.put(key, new Uint8Array(await arrayBuffer(file)));
    if (this.cached.images[key]) URL.revokeObjectURL(this.cached.images[key]);
    this.cached.images[key] = URL.createObjectURL(file);
  };

  getNet = async (key: string | undefined): Promise<Uint8Array | undefined> => {
    if (!key) return undefined;
    await this.ready;
    const cached = this.cached.nets.find(n => n.key === key);
    if (cached) return cached.data;
    const data = await this.db.nets.get(key);
    this.cached.nets.push({ key, data });
    if (this.cached.nets.length > 2) this.cached.nets.shift();
    return data;
  };

  getBook = async (key: string | undefined): Promise<PolyglotBook | undefined> => {
    if (!key) return undefined;
    await this.ready;
    const cached = this.cached.books[key];
    if (cached) return cached;
    const book = await this.db.books
      .get(key)
      .then(arr => site.asset.loadEsm<PolyglotBook>('bits.polyglot', { init: new DataView(arr.buffer) }));
    this.cached.books[key] = book;
    return book;
  };

  getImageUrl = (key: string) => this.cached.images[key] ?? botAssetUrl(`images/${key}`);

  private async setBotEditorAssets() {
    if (!this.assets) return;
    const [idbNets, idbBooks, idbImages] = await Promise.all(
      [this.db.nets, this.db.books, this.db.images].map(s => s.store?.list()),
    );
    const combined = {
      nets: [...new Set([...this.assets.nets, ...(idbNets ?? [])])],
      books: [...new Set([...this.assets.books, ...(idbBooks ?? [])])],
      images: [...new Set([...this.assets.images, ...(idbImages ?? [])])],
    };
    setSchemaAssets(combined);
  }
}

export function botAssetUrl(name: string, version: string | false = 'bot000') {
  return site.asset.url(`lifat/bots/${name}`, { version });
}

function arrayBuffer(file: File): Promise<ArrayBuffer> {
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
    default:
      return 'application/octet-stream';
  }
}

class Store {
  store?: ObjectStorage<Uint8Array, string>;
  constructor(readonly type: string) {
    this.store = undefined;
  }
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
}
