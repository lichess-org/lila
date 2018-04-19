interface Lichess {
  pubsub: Pubsub
  trans(i18n: { [key: string]: string | undefined }): Trans
  numberFormat(n: number): string
  once(key: string): boolean
  quietMode: boolean
  desktopNotification(txt: string | (() => string)): void
  engineName: string;
  assetUrl(url: string, opts?: AssetUrlOpts): string;
  storage: LichessStorageHelper
  reload(): void;
  redirect(o: string | { url: string, cookie: Cookie }): void;
  loadScript(url: string): any
  keyboardMove: any
  slider(): any
  reloadOtherTabs(): void
  raf(f: () => void): void
  requestIdleCallback(f: () => void): void
  loadCss(path: string): void
  unloadCss(path: string): void
  loadedCss: [string];
  escapeHtml(str: string): string
  toYouTubeEmbedUrl(url: string): string
  fp: {
    debounce(func: (...args: any[]) => void, wait: number, immediate?: boolean): (...args: any[]) => void;
    contains<T>(list: T[], needle: T): boolean;
  }
  sound: any
  powertip: any
  userAutocomplete: any
  StrongSocket: {
    sri: string
    (url: string, version: number, cfg: any): any;
  }
  socket: any;
  idleTimer(delay: number, onIdle: () => void, onWakeUp: () => void): void;
  parseFen(el: any): void;
  hasToReload: boolean;
  ab: any;
  challengeApp: any;
  hopscotch: any;
  makeChat(id: string, data: any, callback?: (chat: any) => void): void;
  topMenuIntent(): void;
  timeago: {
    render(nodes: HTMLElement | HTMLElement[]): void;
    format(date: number | Date): string;
    absolute(date: number | Date): string;
  }
  advantageChart: {
    update(data: any): void;
    (data: any, trans: Trans, el: HTMLElement): void;
  }
  dispatchEvent(el: HTMLElement, eventName: string): void;
  isTrident: boolean;
}

interface Cookie {
  name: string;
  value: string;
  maxAge: number;
}

interface AssetUrlOpts {
  sameDomain?: boolean;
  noVersion?: boolean;
}

declare type SocketSend = (type: string, data?: any, opts?: any, noRetry?: boolean) => void;

interface Trans {
  (key: string, ...args: Array<string | number>): string;
  noarg(key: string): string;
  plural(key: string, count: number, ...args: Array<string | number>): string;
  vdom<T>(key: string, ...args: T[]): (string | T)[];
  vdomPlural<T>(key: string, count: number, countArg: T, ...args: T[]): (string | T)[];
}

interface Pubsub {
  on(msg: string, f: (...data: any[]) => void): void
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
  Howl: any
  Chessground: any
  Highcharts: any
  lichessReplayMusic: () => {
    jump(node: Tree.Node): void
  }
  hopscotch: any;
  lichessPlayMusic(): void;

  [key: string]: any; // TODO
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
  validate(bufferSource: ArrayBuffer | Uint8Array): boolean
}

declare var WebAssembly: WebAssemblyStatic | undefined;

declare type VariantKey = 'standard' | 'chess960' | 'antichess' | 'fromPosition' | 'kingOfTheHill' | 'threeCheck' | 'atomic' | 'horde' | 'racingKings' | 'crazyhouse'

declare type Speed = 'bullet' | 'blitz' | 'classical' | 'correspondence' | 'unlimited'

declare type Perf = 'bullet' | 'blitz' | 'classical' | 'correspondence' | 'chess960' | 'antichess' | 'fromPosition' | 'kingOfTheHill' | 'threeCheck' | 'atomic' | 'horde' | 'racingKings' | 'crazyhouse'

declare type Color = 'white' | 'black';

declare type Key = 'a0' | 'a1' | 'b1' | 'c1' | 'd1' | 'e1' | 'f1' | 'g1' | 'h1' | 'a2' | 'b2' | 'c2' | 'd2' | 'e2' | 'f2' | 'g2' | 'h2' | 'a3' | 'b3' | 'c3' | 'd3' | 'e3' | 'f3' | 'g3' | 'h3' | 'a4' | 'b4' | 'c4' | 'd4' | 'e4' | 'f4' | 'g4' | 'h4' | 'a5' | 'b5' | 'c5' | 'd5' | 'e5' | 'f5' | 'g5' | 'h5' | 'a6' | 'b6' | 'c6' | 'd6' | 'e6' | 'f6' | 'g6' | 'h6' | 'a7' | 'b7' | 'c7' | 'd7' | 'e7' | 'f7' | 'g7' | 'h7' | 'a8' | 'b8' | 'c8' | 'd8' | 'e8' | 'f8' | 'g8' | 'h8';
declare type Uci = string;
declare type San = string;
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
    retried?: boolean;
  }

  export interface ServerEval {
    cp?: number;
    mate?: number;
    best?: Uci;
  }

  export interface PvData {
    moves: string[];
    mate?: number;
    cp?: number;
  }

  export interface TablebaseHit {
    winner: Color | undefined;
    best?: Uci;
  }

  export interface Node {
    id: string;
    ply: Ply;
    uci?: Uci;
    fen: Fen;
    children: Node[];
    comments?: Comment[];
    gamebook?: Gamebook;
    dests?: string;
    drops: string | undefined | null;
    check?: boolean;
    threat?: ClientEval;
    ceval?: ClientEval;
    eval?: ServerEval;
    tbhit?: TablebaseHit;
    opening?: Opening;
    glyphs?: Glyph[];
    clock?: Clock;
    parentClock?: Clock;
    shapes?: Shape[];
    comp?: boolean;
    san?: string;
    threefold?: boolean;
    fail?: boolean;
    puzzle?: string;
    crazy?: NodeCrazy;
  }

  export interface NodeCrazy {
    pockets: [CrazyPocket, CrazyPocket];
  }

  export interface CrazyPocket {
    [role: string]: number;
  }

  export interface Comment {
    id: string;
    by: string | {
      id: string;
      name: string;
    };
    text: string;
  }

  export interface Gamebook {
    deviation?: string;
    hint?: string;
    shapes?: Shape[]
  }

  export interface Opening {
    name: string;
    eco: string;
  }

  type GlyphId = number;

  interface Glyph {
    id: GlyphId;
    name: string;
    symbol: string;
  }

  export type Clock = number;

  export interface Shape {
  }
}

interface JQueryStatic {
  modal: LichessModal;
  powerTip: any;
}

interface LichessModal {
  (html: string | JQuery): JQuery;
  close(): void;
}

interface JQuery {
  powerTip(options?: PowerTip.Options | 'show' | 'hide'): JQuery;
  typeahead: any;
  scrollTo(el: JQuery | HTMLElement, delay: number): JQuery;
  sparkline: any;
  clock: any;
  watchers(): JQuery;
  watchers(method: 'set', data: any): void;
  highcharts(conf?: any): any;
}

declare namespace PowerTip {
  type Placement = 'n' | 'e' | 's' | 'w' | 'nw' | 'ne' | 'sw' | 'se' | 'nw-alt' | 'ne-alt' | 'sw-alt' | 'se-alt';

  interface Options {
    followMouse?: boolean;
    mouseOnToPopup?: boolean;
    placement?: Placement;
    smartPlacement?: boolean;
    popupId?: string;
    poupClass?: string;
    offset?: number;
    fadeInTime?: number;
    fadeOutTime?: number;
    closeDelay?: number;
    intentPollInterval?: number;
    intentSensitivity?: number;
    manual?: boolean;
    openEvents?: string[];
    closeEvents?: string[];
  }
}
