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
  partial<T>(f: (...someArgs: any[]) => T): (...args: any[]) => T;
  reload(): void;
  loadScript(url: string): any
  keyboardMove: any
  slider: () => any
  reloadOtherTabs: () => void
  requestIdleCallback(f: () => void): void

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
  Chessground: any
  Highcharts: any
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

interface Math {
  log2?: (x: number) => number;
}

interface WebAssemblyStatic {
  validate: (code: Uint8Array) => boolean;
}

declare var WebAssembly: WebAssemblyStatic | undefined;

declare type VariantKey = 'standard' | 'chess960' | 'antichess' | 'fromPosition' | 'kingOfTheHill' | 'threeCheck' | 'atomic' | 'horde' | 'racingKings' | 'crazyhouse'

declare type Speed = 'bullet' | 'blitz' | 'classical' | 'correspondence' | 'unlimited'

declare type Perf = 'bullet' | 'blitz' | 'classical' | 'correspondence' | 'chess960' | 'antichess' | 'fromPosition' | 'kingOfTheHill' | 'threeCheck' | 'atomic' | 'horde' | 'racingKings' | 'crazyhouse'

declare type Color = 'white' | 'black';

declare type Uci = string;
declare type Fen = string;
declare type Ply = number;

interface Variant {
  key: VariantKey
  name: string
  short: string
  title?: string
}

declare namespace Tree {
  export type Path = string;

  export interface ClientEval {
    fen: Fen;
    maxDepth: number;
    depth: number;
    knps: number;
    nodes: number;
    millis: number;
    pvs: PvData[];
    cloud?: boolean;
    cp?: number;
    mate?: number;
  }

  export interface ServerEval {
    cp?: number;
    mate?: number;
  }

  export interface PvData {
    moves: string[];
    mate?: number;
    cp?: number;
  }

  export interface Node {
    id: string;
    ply: Ply;
    uci: Uci;
    fen: Fen;
    children: Node[];
    comments?: Comment[];
    dests: string | undefined | null;
    drops: string | undefined | null;
    check: boolean;
    threat?: ClientEval;
    ceval?: ClientEval;
    eval?: ServerEval;
    opening?: Opening;
    glyphs?: Glyph[];
    clock?: Clock;
    parentClock?: Clock;
    shapes?: Shape[];
    comp?: boolean;
    san?: string;
    threefold?: boolean;
  }

  export interface Comment {
    id: string;
    text: string;
  }

  export interface Opening {
    eco: string;
    symbol: string;
  }

  export interface Glyph {
    name: string;
    symbol: string;
  }

  export interface Clock {
  }

  export interface Shape {
  }
}
