interface Lidraughts {
  pubsub: Pubsub
  trans(i18n: { [key: string]: string | undefined }): Trans
  numberFormat(n: number): string
  once(key: string): boolean
  quietMode: boolean
  engineName: string;
  assetUrl(url: string, opts?: AssetUrlOpts): string;
  storage: LidraughtsStorageHelper
  reload(): void;
  redirect(o: string | { url: string, cookie: Cookie }): void;
  loadScript(url: string, opts?: AssetUrlOpts): any
  compiledScript(path: string): string
  keyboardMove: any
  slider(): any
  raf(f: () => void): void;
  requestIdleCallback(f: () => void): void;
  loadCss(path: string): void;
  loadCssPath(path: string): void;
  loadedCss: {
    [key: string]: boolean;
  }
  escapeHtml(str: string): string
  debounce(func: (...args: any[]) => void, wait: number, immediate?: boolean): (...args: any[]) => void;
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
  makeChat(data: any, callback?: (chat: any) => void): void;
  timeago: {
    render(nodes: HTMLElement | HTMLElement[]): void;
    format(date: number | Date): string;
    absolute(date: number | Date): string;
  }
  advantageChart: {
    update(data: any): void;
    (data: any, trans: Trans, el: HTMLElement): void;
  }
  dispatchEvent(el: HTMLElement | Window, eventName: string): void;
  RoundNVUI(redraw: () => void): {
    render(ctrl: any): any;
  }
  AnalyseNVUI(redraw: () => void): {
    render(ctrl: any): any;
  }
  playMusic(): any;
  spinnerHtml: string;
  movetimeChart: any;
  hasTouchEvents: boolean;
  mousedownEvent: 'mousedown' | 'touchstart';
  isCol1(): boolean;
  pushSubscribe(ask: boolean): void;
  formAjax(form: JQuery): any;
  reverse(s: string): string;
}

interface LidraughtsSpeech {
  say(t: string, cut: boolean): void;
  step(s: { san?: San, uci?: Uci }, cut: boolean, captureFrom?: Key): void;
}

interface PalantirOpts {
  uid: string;
  redraw(): void;
}
interface Palantir {
  render(h: any): any;
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

type TransNoArg = (key: string) => string;

interface Trans {
  (key: string, ...args: Array<string | number>): string;
  noarg: TransNoArg;
  plural(key: string, count: number, ...args: Array<string | number>): string;
  vdom<T>(key: string, ...args: T[]): (string | T)[];
  vdomPlural<T>(key: string, count: number, countArg: T, ...args: T[]): (string | T)[];
}

type PubsubCallback = (...data: any[]) => void;

interface Pubsub {
  on(msg: string, f: PubsubCallback): void;
  off(msg: string, f: PubsubCallback): void;
  emit(msg: string, ...args: any[]): void;
}

interface LidraughtsStorageHelper {
  make(k: string): LidraughtsStorage;
  makeBoolean(k: string): LidraughtsBooleanStorage;
  get(k: string): string | null;
  set(k: string, v: string): void;
  remove(k: string): void;
}

interface LidraughtsStorage {
  get(): string | null;
  set(v: string): void;
  remove(): void;
  listen(f: (e: StorageEvent) => void): void;
}

interface LidraughtsBooleanStorage {
  get(): boolean;
  set(v: boolean): boolean;
  toggle(): void;
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
  LidraughtsSpeech?: LidraughtsSpeech;
  palantir?: {
    palantir(opts: PalantirOpts): Palantir
  };

  [key: string]: any; // TODO
}

interface LightUser {
  id: string
  name: string
  title?: string
  patron?: boolean
}

interface WebAssemblyStatic {
  validate(bufferSource: ArrayBuffer | Uint8Array): boolean
}

//declare var WebAssembly: WebAssemblyStatic | undefined;

declare type VariantKey = 'standard' | 'antidraughts' | 'breakthrough' | 'fromPosition' | 'frisian' | 'frysk' | 'atomic'

declare type Speed = 'bullet' | 'blitz' | 'classical' | 'correspondence' | 'unlimited'

declare type Perf = 'bullet' | 'blitz' | 'classical' | 'correspondence' | 'antidraughts' | 'breakthrough' | 'fromPosition' | 'frisian' | 'frysk'

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

interface Paginator<A> {
  currentPage: number
  maxPerPage: number
  currentPageResults: Array<A>
  nbResults: number
  previousPage?: number
  nextPage?: number
  nbPages: number
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
    win?: number;
    retried?: boolean;
  }

  export interface ServerEval {
    cp?: number;
    win?: number;
    best?: Uci;
  }

  export interface PvData {
    moves: string[];
    win?: number;
    cp?: number;
  }

  export interface TablebaseHit {
    winner: Color | undefined;
    best?: Uci;
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
    destsUci?: Uci[];
    captLen?: number;
    drops: string | undefined | null;
    alternatives?: Alternative[];
    missingAlts?: Alternative[]; // only used internally
    destreq?: number; // used internally
    check?: boolean;
    threat?: ClientEval;
    ceval?: ClientEval;
    eval?: ServerEval;
    tbhit: TablebaseHit | undefined | null;
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

  export interface Alternative {
    uci: string,
    fen: string
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
  (html: string | JQuery, cls?: string): JQuery;
  close(): void;
}

interface JQuery {
  powerTip(options?: PowerTip.Options | 'show' | 'hide'): JQuery;
  typeahead: any;
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

interface Array<T> {
  includes(t: T): boolean;
}
