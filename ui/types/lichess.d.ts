interface Lichess {
  pubsub: Pubsub
  trans: Trans
  globalTrans(str: string): string
  numberFormat(n: number): string
  once(key: string): boolean
  quietMode: boolean
  desktopNotification(txt: string): void
  engineName: string;
  assetUrl(url: string, opts?: AssetUrlOpts): string;
  storage: LichessStorageHelper

  fp: any
  sound: any
  powertip: any
}

interface AssetUrlOpts {
  sameDomain?: boolean;
  noVersion?: boolean;
}

declare type Trans = any; // todo

interface Pubsub {
  on(msg: string, f: (data: any) => void): void
  emit(msg: string): (...args: any[]) => void
}

interface LichessStorageHelper {
  make(k: string): LichessStorage;
  get(k: string): string;
  set(k: string, v: string): string;
  remove(k: string): void;
}

interface LichessStorage {
  get(): string;
  set(v: string): string;
  remove(): void;
  listen(f: (e: StorageEvent) => void): void;
}

interface Window {
  lichess: Lichess

  moment: any
  Mousetrap: any
}

declare type VariantKey = 'standard' | 'chess960' | 'antichess' | 'fromPosition' | 'kingOfTheHill' | 'threeCheck' | 'atomic' | 'horde' | 'racingKings' | 'crazyhouse'
declare type Speed = 'bullet' | 'blitz' | 'classical' | 'correspondence' | 'unlimited'
declare type Perf = 'bullet' | 'blitz' | 'classical' | 'correspondence' | 'chess960' | 'antichess' | 'fromPosition' | 'kingOfTheHill' | 'threeCheck' | 'atomic' | 'horde' | 'racingKings' | 'crazyhouse'
declare type Color = 'white' | 'black';

interface Variant {
  key: VariantKey
  name: string
  short: string
  title?: string
}

interface Paginator<T> {
  currentPage: number
  maxPerPage: number
  currentPageResults: Array<T>
  nbResults: number
  previousPage: number
  nextPage: number
  nbPages: number
}

interface LightUser {
  id: string
  name: string
  title?: string
  patron?: boolean
}

interface Array<T> {
  find(f: (t: T) => boolean): T | undefined;
}

interface WebAssemblyStatic {
  validate: (code: Uint8Array) => boolean;
}

declare var WebAssembly: WebAssemblyStatic | undefined;
