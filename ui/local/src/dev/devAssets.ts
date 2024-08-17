import { type ObjectStorage, objectStorage } from 'common/objectStorage';
import { botAssetUrl, Assets } from '../assets';
import { makeBookFromPolyglot, makeBookFromPgn, type OpeningBook } from 'bits/polyglot';
import { domDialog, confirm } from 'common/dialog';
import { zip } from 'common';
import { env } from '../localEnv';

// dev asset keys are a 12 digit hex hash of the asset contents (plus the file extension for image/sound)
// dev asset names come from the original filename but can be renamed whatever
// dev asset blobs are stored in idb

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

  readonly server: Record<AssetType, NameMap> = assetTypes.reduce(
    (obj, type) => ({ ...obj, [type]: new Map() }),
    {} as Record<AssetType, NameMap>,
  );

  private idb = assetTypes.reduce(
    (obj, type) => ({ ...obj, [type]: new Store(type) }),
    {} as Record<AssetType, Store>,
  );

  private urls = urlTypes.reduce(
    (obj, type) => ({ ...obj, [type]: new Map() }),
    {} as Record<AssetType, NameMap>,
  );

  constructor(public rlist?: AssetList | undefined) {
    super();
    this.update(rlist);
  }

  async init(): Promise<this> {
    const then = Date.now();
    const [localImages, localSounds, localBookCovers] = await Promise.all(
      ([...urlTypes, 'book'] as const).map(t => this.idb[t].init()),
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

  async update(rlist?: AssetList): Promise<void> {
    if (!rlist) rlist = await fetch('/local/dev/assets').then(res => res.json());
    Object.values(this.server).forEach(m => m.clear());
    assetTypes.forEach(type => rlist?.[type]?.forEach(a => this.server[type].set(a.key, a.name)));
    const books = Object.entries(this.server.book);
    this.server.bookCover = new Map(books.map(([k, v]) => [`${k}.png`, v]));
    assetTypes.forEach(type => (this.server[type] = valueSorted(this.server[type])));
  }

  localMap(type: AssetType): NameMap {
    return this.idb[type].keyNames;
  }

  allMap(type: AssetType): NameMap {
    const allMap = new Map(this.idb[type].keyNames);
    for (const [k, v] of this.server[type]) {
      if (!v.startsWith('.')) allMap.set(k, v);
    }
    return allMap;
  }

  deleted(type: AssetType): string[] {
    return [...this.server[type].entries()].filter(([, v]) => v.startsWith('.')).map(([k]) => k);
  }

  isLocalOnly(key: string): boolean {
    return Boolean(this.traverse(k => k === key, 'local') && !this.traverse(k => k === key, 'server'));
  }

  isDeleted(key: string): boolean {
    for (const map of Object.values(this.server)) {
      if (map.get(key)?.startsWith('.')) return true;
    }
    return false;
  }

  nameOf(key: string): string | undefined {
    return this.traverse(k => k === key)?.[1];
  }

  assetBlob(type: AssetType, key: string): AssetBlob | undefined {
    return this.isLocalOnly(key)
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

  async importBook(pgn: string, studyName: string): Promise<void> {
    const name = (
      await domDialog({
        class: 'alert',
        htmlText: `<div>book name: <input type="text" value="${studyName}"></div>
      <span><button class="button">export</button></span>`,
        actions: {
          selector: 'button',
          listener: async (_, dlg) => {
            const name = (dlg.view.querySelector('input') as HTMLInputElement).value;
            if (!name) dlg.close();
            if ([...this.idb['book'].keyNames.values()].includes(name)) {
              const ok = await confirm('That book already exists. Replace?');
              if (ok) dlg.close(name);
            } else dlg.close(name);
          },
        },
        show: true,
      })
    ).returnValue;
    if (!name || name === 'cancel') return;
    // a study or chapter can be repeatedly updated with the same name after
    // making/testing edits. in that case, we need to patch all bots using that old book to
    // the new key when we import the change because bot.books is a list of keys, not names
    const result = await makeBookFromPgn(pgn, { depth: 10, boardSize: 192 });
    if (!result.polyglot || !result.cover) {
      console.error(result);
      alert('OhNoesError: ' + pgn);
      return;
    }
    const oldKey = [...this.idb.book.keyNames.entries()].find(([, n]) => n === name)?.[0];
    const key = await hashBlob(result.polyglot);
    const asset = { blob: result.polyglot, name, user: env.user };
    const cover = { blob: result.cover, name, user: env.user };
    await Promise.all([this.idb.book.put(key, asset), this.idb.bookCover.put(key, cover)]);
    if (!oldKey || oldKey === key) return;
    const promises: Promise<void>[] = [];
    for (const bot of env.bot.all) {
      const existing = bot.books?.find(b => b.key === oldKey);
      if (existing) {
        existing.key = key;
        promises.push(env.bot.save(bot));
      }
    }
    await Promise.allSettled([...promises, this.idb.book.rm(oldKey), this.idb.bookCover.rm(oldKey)]);
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
      fetch(`/local/dev/asset/mv/${key}/.${encodeURIComponent(this.nameOf(key)!)}`, { method: 'post' }),
      this.clearLocal(type, key),
    ]);
    if (type === 'book') this.clearLocal('bookCover', key);
    if (assetList.status === 'fulfilled') return this.update(await assetList.value.json());
  }

  async rename(type: AssetType, key: string, newName: string): Promise<void> {
    if (this.nameOf(key) === newName) return;
    const [assetList] = await Promise.allSettled([
      fetch(`/local/dev/asset/mv/${key}/${encodeURIComponent(newName)}`, { method: 'post' }),
      this.idb[type].mv(key, newName),
    ]);
    if (assetList.status === 'fulfilled') return this.update(await assetList.value.json());
  }

  async getBook(key: string | undefined): Promise<OpeningBook | undefined> {
    if (!key) return undefined;
    if (key.endsWith('.bin')) key = key.slice(0, -4);
    const cached = this.book.get(key);
    if (cached) return cached;
    const bookBlob = this.idb.book.keyNames.has(key)
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

  private async addBook(filename: string, file: Blob): Promise<string> {
    const data = await arrayBuffer(file);
    const book = await makeBookFromPolyglot(new DataView(data), { depth: 2, boardSize: 192 });
    if (!book.cover) throw new Error(`error parsing ${filename}`);
    const key = await hashBlob(file);
    const name = filename.split('.')[0];
    const asset = { blob: file, name, user: env.user };
    const cover = { blob: book.cover, name, user: env.user };
    await Promise.all([this.idb.book.put(key, asset), this.idb.bookCover.put(key, cover)]);
    this.book.set(key, book.getMoves);
    this.urls.bookCover.set(key, URL.createObjectURL(new Blob([book.cover], { type: 'image/png' })));
    return key;
  }

  private traverse(
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
  return new Map(map ? [...map.entries()].sort((a, b) => a[1].localeCompare(b[1])) : []);
}

type NamedBlob = {
  blob: Blob;
  name: string;
};

class Store {
  private store: ObjectStorage<NamedBlob, string>;

  keyNames = new Map<string, string>();

  constructor(readonly type: AssetType) {}

  async init() {
    this.store = await objectStorage<NamedBlob, string>({ store: `local.${this.type}s` });
    const [keys, assets] = await Promise.all([this.store.list(), this.store.getMany()]);
    const all = zip(keys, assets);
    all.forEach(([k, a]) => this.keyNames.set(k, a.name));
    this.keyNames = valueSorted(this.keyNames);
    return all;
  }

  async put(key: string, value: NamedBlob): Promise<string> {
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

  async get(key: string): Promise<NamedBlob> {
    return await this.store?.get(key);
  }
}
