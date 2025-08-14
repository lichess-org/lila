import { type ObjectStorage, objectStorage } from 'lib/objectStorage';
import { makeBookFromPolyglot, makeBookFromPgn, PgnProgress, PgnFilter } from 'lib/game/polyglot';
import { botAssetUrl } from 'lib/bot/botLoader';
import { alert } from 'lib/view/dialogs';
import { zip } from 'lib/algo';
import { env } from './devEnv';
import { pubsub } from 'lib/pubsub';
import { myUserId } from 'lib';

// dev asset keys are a 12 digit hex hash of the asset contents (plus the file extension for image/sound)
// dev asset names are strictly cosmetic and can be renamed at any time
// dev asset blobs are stored in idb
// asset keys give the filename on server filesystem

export type ShareType = 'image' | 'sound' | 'book';
export type AssetType = ShareType | 'bookCover' | 'net';
export type AssetBlob = { type: AssetType; key: string; name: string; author: string; blob: Promise<Blob> };
export type AssetList = Record<AssetType, Record<string, string>[]>;

const assetTypes = ['image', 'sound', 'book', 'bookCover', 'net'] as const;
const urlTypes = ['image', 'sound', 'bookCover'] as const;

export class DevAssets {
  server: Record<AssetType, Map<string, string>> = assetTypes.reduce(
    (obj, type) => ({ ...obj, [type]: new Map() }),
    {} as Record<AssetType, Map<string, string>>,
  );

  idb: Record<AssetType, Store> = assetTypes.reduce(
    (obj, type) => ({ ...obj, [type]: new Store(type) }),
    {} as Record<AssetType, Store>,
  );

  urls: Record<AssetType, Map<string, string>> = urlTypes.reduce(
    (obj, type) => ({ ...obj, [type]: new Map() }),
    {} as Record<AssetType, Map<string, string>>,
  );

  constructor(public rlist?: AssetList | undefined) {
    this.update(rlist);
    window.addEventListener('storage', this.onStorageEvent);
  }

  async init(): Promise<this> {
    localStorage.removeItem('botdev.import.book');
    for (const type of urlTypes) {
      for (const url of this.urls[type].values()) {
        URL.revokeObjectURL(url);
      }
      this.urls[type].clear();
    }
    const [localImages, localSounds, localBookCovers] = await Promise.all(
      ([...urlTypes, 'book'] as const).map(t => this.idb[t].init()),
    );
    const urlAssets = { image: localImages, sound: localSounds, bookCover: localBookCovers };
    urlTypes.forEach(type => {
      for (const [key, data] of urlAssets[type]) {
        this.urls[type].set(key, URL.createObjectURL(new Blob([data.blob], { type: mimeOf(key) })));
      }
    });
    return this;
  }

  localKeyNames(type: AssetType): Map<string, string> {
    return this.idb[type].keyNames;
  }

  serverKeyNames(type: AssetType): Map<string, string> {
    return this.server[type];
  }

  allKeyNames(type: AssetType): Map<string, string> {
    const allMap = new Map(this.idb[type].keyNames);
    for (const [k, v] of this.server[type]) {
      if (!v.startsWith('.')) allMap.set(k, v);
    }
    return allMap;
  }

  deletedKeys(type: AssetType): string[] {
    return [...this.server[type].entries()].filter(([, v]) => v.startsWith('.')).map(([k]) => k);
  }

  isLocalOnly(key: string): boolean {
    return Boolean(this.find(k => k === key, 'local') && !this.find(k => k === key, 'server'));
  }

  isDeleted(key: string): boolean {
    for (const map of Object.values(this.server)) {
      if (map.get(key)?.startsWith('.')) return true;
    }
    return false;
  }

  nameOf(key: string): string | undefined {
    return this.find(k => k === key)?.[1];
  }

  assetBlob(type: AssetType, key: string): AssetBlob | undefined {
    if (this.isLocalOnly(key))
      return {
        key,
        type,
        author: myUserId() ?? 'anonymous',
        name: this.idb[type].keyNames.get(key) ?? key,
        blob: this.idb[type].get(key).then(data => data.blob),
      };
    else return undefined;
  }

  async import(type: AssetType, blobname: string, blob: Blob): Promise<string> {
    if (type === 'net' || type === 'book') throw new Error('no');
    const extpos = blobname.lastIndexOf('.');
    if (extpos === -1) throw new Error('filename must have extension');
    const [name, ext] = [blobname.slice(0, extpos), blobname.slice(extpos + 1)];
    const key = `${await hashBlob(blob)}.${ext}`;
    await this.idb[type].put(key, { blob, name, user: myUserId() ?? 'anonymous' });
    if (!this.urls[type].has(key)) this.urls[type].set(key, URL.createObjectURL(blob));
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
    const [assetList] = await Promise.allSettled([
      fetch(`/bots/dev/asset/mv/${key}/.${encodeURIComponent(this.nameOf(key)!)}`, { method: 'post' }),
      this.clearLocal(type, key),
    ]);
    if (type === 'book') this.clearLocal('bookCover', key);
    if (assetList.status === 'fulfilled') return this.update(await assetList.value.json());
  }

  async rename(type: AssetType, key: string, newName: string): Promise<void> {
    if (this.nameOf(key) === newName) return;
    const [assetList] = await Promise.allSettled([
      fetch(`/bots/dev/asset/mv/${key}/${encodeURIComponent(newName)}`, { method: 'post' }),
      this.idb[type].mv(key, newName),
    ]);
    if (assetList.status === 'fulfilled') return this.update(await assetList.value.json());
  }

  getBookCoverUrl(key: string): string {
    return this.urls.bookCover.get(key) ?? botAssetUrl('book', `${key}.png`);
  }

  async importPolyglot(blobname: string, blob: Blob): Promise<string> {
    if (blob.type !== 'application/octet-stream') throw new Error('no');
    const data = await blobArrayBuffer(blob);
    const book = await makeBookFromPolyglot({ bytes: new DataView(data), cover: true });
    if (!book.cover) throw new Error(`error parsing ${blobname}`);
    const key = await hashBlob(blob);
    const name = blobname.endsWith('.bin') ? blobname.slice(0, -4) : blobname;
    const asset = { blob: blob, name, user: myUserId() ?? 'anonymous' };
    const cover = { blob: book.cover, name, user: myUserId() ?? 'anonymous' };
    await Promise.all([this.idb.book.put(key, asset), this.idb.bookCover.put(key, cover)]);
    this.urls.bookCover.set(key, URL.createObjectURL(new Blob([book.cover], { type: 'image/png' })));
    return key;
  }

  async importPgn(
    blobname: string,
    pgn: Blob,
    ply: number,
    fromStudy: boolean,
    progress?: PgnProgress,
    filter?: PgnFilter,
  ): Promise<string | undefined> {
    // a study can be repeatedly imported with the same name during the play balancing cycle. in
    // that case, we need to patch all bots using the key associated with the previous version to
    // the new key at the time we import the change because it's tough for a user to figure out later.
    const name = blobname.endsWith('.pgn') ? blobname.slice(0, -4) : blobname;
    const result = await makeBookFromPgn({ pgn, ply, cover: true, progress, filter });
    if (!result.positions || !result.polyglot || !result.cover) {
      console.log(result, 'cancelled?');
      return undefined;
    }
    const oldKey = [...this.idb.book.keyNames.entries()].find(([, n]) => n === name)?.[0];
    const key = await hashBlob(result.polyglot);
    const asset = { blob: result.polyglot, name, user: myUserId() ?? 'anonymous' };
    const cover = { blob: result.cover, name, user: myUserId() ?? 'anonymous' };
    await Promise.all([this.idb.book.put(key, asset), this.idb.bookCover.put(key, cover)]);

    const promises: Promise<void>[] = [];
    if (oldKey && oldKey !== key) {
      for (const bot of env.bot.all) {
        const existing = bot.books?.find(b => b.key === oldKey);
        if (existing) {
          existing.key = key;
          promises.push(env.bot.storeBot(bot));
        }
      }
      await Promise.allSettled([...promises, this.idb.book.rm(oldKey), this.idb.bookCover.rm(oldKey)]);
    }
    if (fromStudy) {
      localStorage.setItem('botdev.import.book', `${key}${oldKey ? ',' + oldKey : ''}`);
      alert(`${name} exported to bot studio. ${promises.length ? ` ${promises.length} bots updated` : ''}`);
    } else {
      this.urls.bookCover.set(key, URL.createObjectURL(new Blob([cover.blob], { type: 'image/png' })));
      pubsub.emit('botdev.import.book', key, oldKey);
      if (promises.length) alert(`updated ${promises.length} bots with new ${name}`);
    }
    return key;
  }

  async update(rlist?: AssetList): Promise<void> {
    if (!rlist) rlist = await fetch('/bots/dev/assets').then(res => res.json());
    Object.values(this.server).forEach(m => m.clear());
    this.server.book.set('lichess', 'lichess');
    assetTypes.forEach(type => rlist?.[type]?.forEach(a => this.server[type].set(a.key, a.name)));
    const books = Object.entries(this.server.book);
    this.server.bookCover = new Map(books.map(([k, v]) => [`${k}.png`, v]));
    assetTypes.forEach(type => (this.server[type] = valueSorted(this.server[type])));
  }

  private onStorageEvent = async (e: StorageEvent) => {
    if (e.key !== 'botdev.import.book' || !e.newValue) return;

    await this.init();
    const [key, oldKey] = e.newValue.split(',');
    pubsub.emit('botdev.import.book', key, oldKey);
  };

  private find(
    fn: (key: string, name: string, type: AssetType) => boolean,
    maps: 'local' | 'server' | 'both' = 'both',
  ): [key: string, name: string, type: AssetType] | undefined {
    for (const type of assetTypes) {
      if (maps !== 'server')
        for (const [key, name] of this.idb[type].keyNames) {
          if (fn(key, name, type)) return [key, name, type];
        }
      if (maps === 'local') continue;
      for (const [key, name] of this.server[type]) {
        if (fn(key, name, type)) return [key, name, type];
      }
    }
    return undefined;
  }
}

type IdbAsset = { blob: Blob; name: string; user: string };

class Store {
  private store: ObjectStorage<IdbAsset, string>;

  keyNames: Map<string, string> = new Map();

  constructor(readonly type: AssetType) {}

  async init(): Promise<any> {
    this.keyNames.clear();
    this.store = await objectStorage<IdbAsset, string>({ store: `botdev.${this.type}` });
    const [keys, assets] = await Promise.all([this.store.list(), this.store.getMany()]);
    const all = zip(keys, assets);
    all.forEach(([k, a]) => this.keyNames.set(k, a.name));
    this.keyNames = valueSorted(this.keyNames);
    return all;
  }

  async put(key: string, value: IdbAsset): Promise<string> {
    this.keyNames.set(key, value.name);
    return await this.store.put(key, value);
  }

  async rm(key: string): Promise<void> {
    await this.store.remove(key);
    this.keyNames.delete(key);
  }

  async mv(key: string, newName: string): Promise<void> {
    if (this.keyNames.get(key) === newName) return;
    const asset = await this.store.get(key);
    if (!asset) return;
    this.keyNames.set(key, newName);
    await this.store.put(key, { ...asset, name: newName });
  }

  async get(key: string): Promise<IdbAsset> {
    return await this.store?.get(key);
  }
}

function valueSorted(map: Map<string, string> | undefined) {
  return new Map(map ? [...map.entries()].sort((a, b) => a[1].localeCompare(b[1])) : []);
}

async function hashBlob(file: Blob): Promise<string> {
  const hashBuffer = await window.crypto.subtle.digest('SHA-256', await blobArrayBuffer(file));
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray
    .map(b => b.toString(16).padStart(2, '0'))
    .join('')
    .slice(0, 12);
}

function blobArrayBuffer(file: Blob): Promise<ArrayBuffer> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as ArrayBuffer);
    reader.onerror = reject;
    reader.readAsArrayBuffer(file);
  });
}

// function blobString(file: Blob): Promise<string> {
//   return new Promise((resolve, reject) => {
//     const reader = new FileReader();
//     reader.onload = () => resolve(reader.result as string);
//     reader.onerror = reject;
//     reader.readAsText(file);
//   });
// }

function mimeOf(filename: string) {
  // go live with webp and mp3 only, but support more formats during dev work
  switch (filename.slice(filename.lastIndexOf('.') + 1).toLowerCase()) {
    case 'jpg':
    case 'jpeg':
      return 'image/jpeg';
    case 'png':
      return 'image/png';
    case 'webp':
      return 'image/webp';
    case 'aac':
      return 'audio/aac';
    case 'mp3':
      return 'audio/mpeg';
    case 'pgn':
      return 'application/x-chess-pgn';
    case 'bin':
      return 'application/octet-stream';
  }
  return undefined;
}
