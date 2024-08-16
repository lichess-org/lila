import { type ObjectStorage, objectStorage } from 'common/objectStorage';
import { botAssetUrl, Assets } from '../assets';
import { makeBookFromPolyglot, type OpeningBook } from 'bits/polyglot';
import { type ShareCtrl } from './shareCtrl';
import { type BotCtrl } from '../botCtrl';
import { zip } from 'common';
import { env } from '../localEnv';

// asset keys are a 12 digit hex hash of the asset contents (plus the file extension for image/sound)
// asset names come from the original filename but can be renamed whatever
// asset blobs are stored in indexeddb

export type ShareType = 'image' | 'sound' | 'book';
export type AssetType = ShareType | 'bookCover' | 'net';

const assetTypes: AssetType[] = ['image', 'sound', 'book', 'bookCover', 'net'];
const urlTypes = ['image', 'sound', 'bookCover'] as const;

export type AssetBlob = { type: AssetType; key: string; name: string; blob: Promise<Blob> };

type NameMap = Map<string, string>;
type KeyName = { key: string; name: string };
export type AssetList = Record<AssetType, KeyName[]>;

export class DevAssets extends Assets {
  readonly user: string = document.body.getAttribute('data-user') ?? 'Anonymous';

  private idb = assetTypes.reduce(
    (obj, type) => ({ ...obj, [type]: new Store(type) }),
    {} as Record<AssetType, Store>,
  );

  private server = assetTypes.reduce(
    (obj, type) => ({ ...obj, [type]: new Map() }),
    {} as Record<AssetType, NameMap>,
  );

  private urls = urlTypes.reduce(
    (obj, type) => ({ ...obj, [type]: new Map() }),
    {} as Record<AssetType, NameMap>,
  );

  constructor(public rlist: AssetList) {
    super();
    this.update(rlist);
  }

  get dev() {
    return true;
  }

  async init(): Promise<this> {
    const then = Date.now();
    const [localImages, localSounds, localBookCovers] = await Promise.all(
      urlTypes.map(t => this.idb[t].init()),
    );
    const urlAssets = { image: localImages, sound: localSounds, bookCover: localBookCovers };
    urlTypes.forEach(type => {
      for (const [key, data] of urlAssets[type]) {
        this.urls[type].set(key, URL.createObjectURL(new Blob([data.blob], { type: extToMime(key) })));
      }
    });
    console.log('DevAssets init', Date.now() - then, 'ms');
    return this;
  }

  update(rlist: AssetList): void {
    Object.values(this.server).forEach(m => m.clear());
    assetTypes.forEach(type => rlist[type]?.forEach(a => this.server[type].set(a.key, a.name)));
    const books = Object.entries(this.server.book);
    this.server.bookCover = new Map(books.map(([k, v]) => [`${k}.png`, v]));
    assetTypes.forEach(type => (this.server[type] = valueSorted(this.server[type])));
    console.log(this.server);
  }

  localKeys(type: AssetType): string[] {
    if (type === 'net') return [];
    return [...this.idb[type].keys].filter(k => !this.server[type].has(k));
  }

  localNames(type: AssetType): NameMap {
    return structuredClone(this.idb[type].keyNames);
  }

  isLocalOnly(type: AssetType, key: string): boolean {
    return this.idb[type].keys.includes(key) && !this.server[type].has(key);
  }

  serverNames(type: AssetType): NameMap {
    return structuredClone(this.server[type]);
  }

  allKeys(type: AssetType): string[] {
    return [...new Set([...this.server[type].keys(), ...this.idb[type].keys])];
  }

  all(t: AssetType): NameMap {
    const allMap = this.idb[t].keyNames;
    this.server[t].forEach((v, k) => allMap.set(k, v));
    return allMap;
  }

  nameOf(key: string): string | undefined {
    for (const map of Object.values(this.server)) {
      if (map.has(key)) return map.get(key);
    }
    for (const store of Object.values(this.idb)) {
      if (store.keyNames.has(key)) return store.keyNames.get(key);
    }
    return undefined;
  }

  assetBlob(type: AssetType, key: string): AssetBlob | undefined {
    return this.isLocalOnly(type, key)
      ? {
          key,
          type,
          name: this.idb[type].keyNames.get(key) ?? key,
          blob: this.idb[type].get(key).then(data => data.blob),
        }
      : undefined;
  }

  async add(type: AssetType, filename: string, file: Blob): Promise<string> {
    if (type === 'net') throw new Error('no');
    if (type === 'book') return this.addBook(filename, file);
    const extpos = filename.lastIndexOf('.');
    if (extpos === -1) throw new Error('filename must have extension');
    const [name, ext] = [filename.slice(0, extpos), filename.slice(extpos + 1)];
    const key = `${await hashBlob(file)}.${ext}`;
    const asset = { blob: file, name, user: env.user };
    await this.idb[type].put(key, asset);
    if (type === 'image' || type === 'sound') {
      const oldUrl = this.urls[type].get(key);
      if (oldUrl) URL.revokeObjectURL(oldUrl);
      this.urls[type].set(key, URL.createObjectURL(file));
    }
    return key;
  }

  async addBook(filename: string, file: Blob): Promise<string> {
    const data = await arrayBuffer(file);
    const book = await makeBookFromPolyglot(new DataView(data), { depth: 2, boardSize: 192 });
    if (!book.cover) throw new Error(`error parsing ${filename}`);
    const key = await hashBlob(file);
    const name = filename.split('.')[0];
    const asset = { blob: file, name, user: env.user };
    const cover = { blob: book.cover, name: `${name}.png`, user: env.user };
    await Promise.all([this.idb.book.put(key, asset), this.idb.bookCover.put(key, cover)]);
    this.book.set(key, book.getMoves);
    this.urls.bookCover.set(key, URL.createObjectURL(new Blob([book.cover], { type: 'image/png' })));
    return key;
  }

  async clearLocal(type: AssetType, key: string): Promise<void> {
    await this.idb[type].rm(key);
    if (type === 'image' || type === 'sound' || type === 'bookCover') {
      const oldUrl = this.urls[type].get(key);
      if (oldUrl) URL.revokeObjectURL(oldUrl);
      this.urls[type].delete(key);
    }
  }

  async delete(type: AssetType, key: string): Promise<void> {
    if (type === 'net') throw new Error('no');
    await Promise.allSettled([this.clearLocal(type, key), fetch(`/local/dev/asset/rm/${key}`)]);
    if (type === 'book') this.clearLocal('bookCover', key);
  }

  async renameAsset(type: AssetType, key: string, newName: string): Promise<void> {
    if (this.nameOf(key) === newName) return;
    await Promise.allSettled([
      this.idb[type].mv(key, newName),
      fetch(`/local/dev/asset/mv/${key}/${encodeURIComponent(newName)}`),
    ]);
  }

  async getBook(key: string | undefined): Promise<OpeningBook | undefined> {
    if (!key) return undefined;
    if (key.endsWith('.bin')) key = key.slice(0, -4);
    const cached = this.book.get(key);
    if (cached) return cached;
    const bookBlob = [...this.idb.book.keys].includes(key)
      ? (await this.idb.book.get(key)).blob
      : await fetch(botAssetUrl('book', `${key}.bin`, false)).then(res => res.blob());
    const bytes = new DataView(await bookBlob.arrayBuffer());
    const book = await makeBookFromPolyglot(bytes);
    this.book.set(key, book.getMoves);
    return book.getMoves;
  }

  getImageUrl(key: string): string {
    return this.urls.image.get(key) ?? botAssetUrl('image', key, false);
  }

  getSoundUrl(key: string): string {
    return this.urls.sound.get(key) ?? botAssetUrl('sound', key, false);
  }

  getBookCoverUrl(key: string): string {
    return this.urls.bookCover.get(key) ?? botAssetUrl('book', `${key}.png`, false);
  }
}

async function hashBlob(file: Blob): Promise<string> {
  const hashBuffer = await window.crypto.subtle.digest('SHA-256', await arrayBuffer(file));
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray
    .map(b => b.toString(16).padStart(2, '0'))
    .join('')
    .slice(0, 12);
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

function valueSorted(map: NameMap | undefined) {
  if (!map) return new Map();
  return new Map([...map].sort((a, b) => a[1].localeCompare(b[1])));
}

type NamedBlob = {
  blob: Blob;
  name: string;
};

class Store {
  private store: ObjectStorage<NamedBlob, string>;

  keyNames = new Map<string, string>();

  constructor(readonly type: AssetType) {}

  get keys(): string[] {
    return [...this.keyNames.keys()];
  }

  async init() {
    this.store = await objectStorage<NamedBlob, string>({ store: `local.${this.type}s` });
    const all = await this.getAll();
    all.forEach(([k, a]) => this.keyNames.set(k, a.name));
    this.keyNames = valueSorted(this.keyNames);
    return all;
  }

  async put(key: string, value: NamedBlob): Promise<string> {
    this.keyNames.set(key, value.name);
    return await this.store.put(key, value);
  }

  async getAll(): Promise<[string, NamedBlob][]> {
    const [keys, assets] = await Promise.all([this.store.list(), this.store.getMany()]);
    return zip(keys, assets);
  }

  async rm(key: string): Promise<void> {
    await this.store.remove(key);
    this.keyNames.delete(key);
  }

  async mv(key: string, newName: string): Promise<void> {
    if (this.keyNames.get(key) === newName) return;
    const asset = await this.store.get(key);
    this.keyNames.set(key, newName);
    await this.store.put(key, { ...asset, name: newName });
  }

  async get(key: string): Promise<NamedBlob> {
    return await this.store?.get(key);
  }

  name(key: string): string | undefined {
    return this.keyNames.get(key);
  }
}
