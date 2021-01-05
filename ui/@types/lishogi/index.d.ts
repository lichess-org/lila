interface Lishogi {
  // standalones/util.js
  requestIdleCallback(f: () => void): void;
  dispatchEvent(el: HTMLElement | Window, eventName: string): void;
  hasTouchEvents: boolean;
  sri: string;
  isCol1(): boolean;
  storage: LishogiStorageHelper;
  tempStorage: LishogiStorageHelper; // TODO: unused
  once(key: string, mod?: "always"): boolean;
  debounce(
    func: (...args: any[]) => void,
    wait: number,
    immediate?: boolean
  ): (...args: any[]) => void;
  powertip: any;
  widget: unknown;
  hoverable?: boolean;
  isHoverable(): boolean;
  spinnerHtml: string;
  assetUrl(url: string, opts?: AssetUrlOpts): string;
  loadedCss: { [key: string]: boolean };
  loadCss(path: string): void;
  loadCssPath(path: string): void;
  compiledScript(path: string): string;
  loadScript(url: string, opts?: AssetUrlOpts): Promise<unknown>;
  hopscotch: any;
  slider(): any;
  makeChat(data: any, callback?: (chat: any) => void): void;
  formAjax(form: JQuery): any;
  numberFormat(n: number): string;
  idleTimer(delay: number, onIdle: () => void, onWakeUp: () => void): void;
  pubsub: Pubsub;
  hasToReload: boolean;
  redirect(o: string | { url: string; cookie: Cookie }): void;
  reload(): void;
  escapeHtml(str: string): string;
  announce(d: LishogiAnnouncement): void;

  // standalones/trans.js
  trans(i18n: { [key: string]: string | undefined }): Trans;

  // main.js
  socket: any;
  reverse(s: string): string;
  sound: any;
  userAutocomplete: any;
  parseFen(el: any): void;
  challengeApp: any;
  ab?: any;

  // socket.js
  StrongSocket: {
    (url: string, version: number | false, cfg: any): any;
    defaults: {
      events: {
        fen(e: any): void;
      };
    };
  };

  // timeago.js
  timeago: {
    render(nodes: HTMLElement | HTMLElement[]): void;
    format(date: number | Date): string;
    absolute(date: number | Date): string;
  };

  // misc
  advantageChart: {
    update(data: any): void;
    (data: any, trans: Trans, el: HTMLElement, notation: any): void;
  };
  movetimeChart: any;
  RoundNVUI(
    redraw: () => void
  ): {
    render(ctrl: any): any;
  };
  AnalyseNVUI(
    redraw: () => void
  ): {
    render(ctrl: any): any;
  };
  playMusic(): any;
  quietMode?: boolean;
  keyboardMove?: any;
  notifyApp: {
    setMsgRead(user: string): void;
  };
}

interface LishogiSpeech {
  say(t: string, cut: boolean): void;
  step(s: { san?: San }, cut: boolean): void;
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

declare type SocketSend = (
  type: string,
  data?: any,
  opts?: any,
  noRetry?: boolean
) => void;

type TransNoArg = (key: string) => string;

interface Trans {
  (key: string, ...args: Array<string | number>): string;
  noarg: TransNoArg;
  plural(key: string, count: number, ...args: Array<string | number>): string;
  vdom<T>(key: string, ...args: T[]): (string | T)[];
  vdomPlural<T>(
    key: string,
    count: number,
    countArg: T,
    ...args: T[]
  ): (string | T)[];
}

type PubsubCallback = (...data: any[]) => void;

interface Pubsub {
  on(msg: string, f: PubsubCallback): void;
  off(msg: string, f: PubsubCallback): void;
  emit(msg: string, ...args: any[]): void;
}

interface LishogiStorageHelper {
  make(k: string): LishogiStorage;
  makeBoolean(k: string): LishogiBooleanStorage;
  get(k: string): string | null;
  set(k: string, v: string): void;
  fire(k: string, v?: string): void;
  remove(k: string): void;
}

interface LishogiStorage {
  get(): string | null;
  set(v: any): void;
  remove(): void;
  listen(f: (e: LishogiStorageEvent) => void): void;
  fire(v?: string): void;
}

interface LishogiBooleanStorage {
  get(): boolean;
  set(v: boolean): boolean;
  toggle(): void;
}

interface LishogiStorageEvent {
  sri: string;
  nonce: number;
  value?: string;
}

interface LishogiAnnouncement {
  msg?: string;
  date?: string;
}

interface Window {
  lishogi: Lishogi;

  moment: any;
  Mousetrap: any;
  Howl: any;
  Shogiground: any;
  Highcharts: any;
  lishogiReplayMusic: () => {
    jump(node: Tree.Node): void;
  };
  hopscotch: any;
  LishogiSpeech?: LishogiSpeech;
  palantir?: {
    palantir(opts: PalantirOpts): Palantir;
  };

  [key: string]: any; // TODO
}

interface LightUser {
  id: string;
  name: string;
  title?: string;
  patron?: boolean;
}

declare var SharedArrayBuffer: any | undefined;
declare var Atomics: any | undefined;

interface Navigator {
  deviceMemory: number;
}

declare type VariantKey =
  | "standard"
  | "chess960"
  | "antichess"
  | "fromPosition"
  | "kingOfTheHill"
  | "threeCheck"
  | "atomic"
  | "horde"
  | "racingKings"
  | "crazyhouse";

declare type Speed =
  | "bullet"
  | "blitz"
  | "classical"
  | "correspondence"
  | "unlimited";

declare type Perf =
  | "bullet"
  | "blitz"
  | "classical"
  | "correspondence"
  | "chess960"
  | "antichess"
  | "fromPosition"
  | "kingOfTheHill"
  | "threeCheck"
  | "atomic"
  | "horde"
  | "racingKings"
  | "crazyhouse";

declare type Color = "white" | "black";

declare type Key =
  | "a0"
  | "a1"
  | "b1"
  | "c1"
  | "d1"
  | "e1"
  | "f1"
  | "g1"
  | "h1"
  | "i1"
  | "a2"
  | "b2"
  | "c2"
  | "d2"
  | "e2"
  | "f2"
  | "g2"
  | "h2"
  | "i2"
  | "a3"
  | "b3"
  | "c3"
  | "d3"
  | "e3"
  | "f3"
  | "g3"
  | "h3"
  | "i3"
  | "a4"
  | "b4"
  | "c4"
  | "d4"
  | "e4"
  | "f4"
  | "g4"
  | "h4"
  | "i4"
  | "a5"
  | "b5"
  | "c5"
  | "d5"
  | "e5"
  | "f5"
  | "g5"
  | "h5"
  | "i5"
  | "a6"
  | "b6"
  | "c6"
  | "d6"
  | "e6"
  | "f6"
  | "g6"
  | "h6"
  | "i6"
  | "a7"
  | "b7"
  | "c7"
  | "d7"
  | "e7"
  | "f7"
  | "g7"
  | "h7"
  | "i7"
  | "a8"
  | "b8"
  | "c8"
  | "d8"
  | "e8"
  | "f8"
  | "g8"
  | "h8"
  | "i8"
  | "a9"
  | "b9"
  | "c9"
  | "d9"
  | "e9"
  | "f9"
  | "g9"
  | "h9"
  | "i9";
declare type Uci = string;
declare type San = string;
declare type Fen = string;
declare type Ply = number;

interface Variant {
  key: VariantKey;
  name: string;
  short: string;
  title?: string;
}

interface Paginator<A> {
  currentPage: number;
  maxPerPage: number;
  currentPageResults: Array<A>;
  nbResults: number;
  previousPage?: number;
  nextPage?: number;
  nbPages: number;
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
    drops?: string | null;
    check?: Key;
    threat?: ClientEval;
    ceval?: ClientEval;
    eval?: ServerEval;
    tbhit?: TablebaseHit | null;
    glyphs?: Glyph[];
    clock?: Clock;
    parentClock?: Clock;
    forceVariation?: boolean;
    shapes?: Shape[];
    comp?: boolean;
    san?: string;
    threefold?: boolean;
    fail?: boolean;
    puzzle?: "win" | "fail" | "good" | "retry";
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
    by:
      | string
      | {
          id: string;
          name: string;
        };
    text: string;
  }

  export interface Gamebook {
    deviation?: string;
    hint?: string;
    shapes?: Shape[];
  }

  type GlyphId = number;

  interface Glyph {
    id: GlyphId;
    name: string;
    symbol: string;
  }

  export type Clock = number;

  export interface Shape {}
}

interface JQueryStatic {
  modal: LishogiModal;
  powerTip: any;
}

interface LishogiModal {
  (html: string | JQuery, cls?: string, onClose?: () => void): JQuery;
  close(): void;
}

interface JQuery {
  powerTip(options?: PowerTip.Options | "show" | "hide"): JQuery;
  typeahead: any;
  sparkline: any;
  clock: any;
  watchers(): JQuery;
  watchers(method: "set", data: any): void;
  highcharts(conf?: any): any;
  slider(key: string, value: any): any;
  slider(opts: any): any;
  flatpickr(opts: any): any;
}

declare namespace PowerTip {
  type Placement =
    | "n"
    | "e"
    | "s"
    | "w"
    | "nw"
    | "ne"
    | "sw"
    | "se"
    | "nw-alt"
    | "ne-alt"
    | "sw-alt"
    | "se-alt";

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
