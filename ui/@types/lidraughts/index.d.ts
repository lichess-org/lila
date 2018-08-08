interface Lidraughts {
  pubsub: Pubsub
  trans(i18n: { [key: string]: string | undefined }): Trans
  numberFormat(n: number): string
  once(key: string): boolean
  quietMode: boolean
  desktopNotification(txt: string | (() => string)): void
  engineName: string;
  assetUrl(url: string, opts?: AssetUrlOpts): string;
  storage: LidraughtsStorageHelper
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

interface LidraughtsStorageHelper {
  make(k: string): LidraughtsStorage;
  get(k: string): string;
  set(k: string, v: string): string;
  remove(k: string): void;
}

interface LidraughtsStorage {
  get(): string;
  set(v: string): string;
  remove(): void;
  listen(f: (e: StorageEvent) => void): void;
}

interface Window {
  lidraughts: Lidraughts

  moment: any
  Mousetrap: any
  Howl: any
  Draughtsground: any
  Highcharts: any
  lidraughtsReplayMusic: () => {
    jump(node: Tree.Node): void
  }
  hopscotch: any;
  lidraughtsPlayMusic(): void;

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

declare type VariantKey = 'standard' | 'chess960' | 'antichess' | 'fromPosition' | 'kingOfTheHill' | 'threeCheck' | 'atomic' | 'horde' | 'racingKings' | 'crazyhouse' | 'frisian'

declare type Speed = 'bullet' | 'blitz' | 'classical' | 'correspondence' | 'unlimited'

declare type Perf = 'bullet' | 'blitz' | 'classical' | 'correspondence' | 'chess960' | 'antichess' | 'fromPosition' | 'kingOfTheHill' | 'threeCheck' | 'atomic' | 'horde' | 'racingKings' | 'crazyhouse'

declare type Color = 'white' | 'black';

declare type Key = "00" | "01" | "02" | "03" | "04" | "05" | "06" | "07" | "08" | "09" | "10" | "11" | "12" | "13" | "14" | "15" | "16" | "17" | "18" | "19" | "20" | "21" | "22" | "23" | "24" | "25" | "26" | "27" | "28" | "29" | "30" | "31" | "32" | "33" | "34" | "35" | "36" | "37" | "38" | "39" | "40" | "41" | "42" | "43" | "44" | "45" | "46" | "47" | "48" | "49" | "50";
declare type Uci = string;
declare type San = string;
declare type Fen = string;
declare type Ply = number;

interface Variant {
  key: VariantKey
  name: string
  short: string
  title?: string
  gameType?: string
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

  export interface Node {
    id: string;
    ply: Ply;
    displayPly?: Ply;
    uci?: Uci;
    fen: Fen;
    children: Node[];
	mergedNodes?: Node[];
    comments?: Comment[];
    gamebook?: Gamebook;
    dests?: string;
    captLen?: number;
    drops: string | undefined | null;
    check?: boolean;
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
    expandedSan?: string;
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
  modal: LidraughtsModal;
  powerTip: any;
}

interface LidraughtsModal {
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
