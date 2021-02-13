interface Lichess {
  load: Promise<unknown>; // window.onload promise
  info: any;
  requestIdleCallback(f: () => void): void;
  sri: string;
  storage: LichessStorageHelper;
  tempStorage: LichessStorageHelper;
  once(key: string, mod?: 'always'): boolean;
  powertip: any;
  widget: any;
  spinnerHtml: string;
  assetUrl(url: string, opts?: AssetUrlOpts): string;
  soundUrl: string;
  loadCss(path: string): void;
  loadCssPath(path: string): void;
  jsModule(name: string): string;
  loadScript(url: string, opts?: AssetUrlOpts): Promise<void>;
  hopscotch: any;
  slider(): Promise<void>;
  makeChat(data: any): any;
  numberFormat(n: number): string;
  idleTimer(delay: number, onIdle: () => void, onWakeUp: () => void): void;
  pubsub: Pubsub;
  unload: {
    expected: boolean;
  };
  watchers(el: HTMLElement): void;
  redirect(o: RedirectTo): void;
  reload(): void;
  escapeHtml(str: string): string;
  announce(d: LichessAnnouncement): void;

  trans(i18n: { [key: string]: string | undefined }): Trans
  quantity(n: number): 'zero' | 'one' | 'few' | 'many' | 'other';

  socket: any;
  sound: any;
  soundBox: SoundBoxI;
  userAutocomplete($input: JQuery, opts?: UserAutocompleteOpts): Promise<void>;
  miniBoard: {
    init(node: HTMLElement): void;
    initAll(): void;
  };
  miniGame: {
    init(node: HTMLElement): string | null;
    initAll(): void;
    update(node: HTMLElement, data: { fen: string, lm: string, wc?: number, bc?: number }): void;
    finish(node: HTMLElement, win?: Color): void;
  };
  ab?: any;

  // socket.js
  StrongSocket: {
    new(url: string, version: number | false, cfg?: any): any;
    firstConnect: Promise<(tpe: string, data: any) => void>
  }

  // timeago.js
  timeago: {
    render(nodes: HTMLElement | HTMLElement[]): void;
    format(date: number | Date): string;
    absolute(date: number | Date): string;
  }
  timeagoLocale(a: number, b: number, c: number): any;

  // misc
  advantageChart: {
    update(data: any): void;
    (data: any, trans: Trans, el: HTMLElement): void;
  }
  movetimeChart: any;
  RoundNVUI(redraw: () => void): {
    render(ctrl: any): any;
  }
  AnalyseNVUI(redraw: () => void): {
    render(ctrl: any): any;
  }
  playMusic(): any;
  quietMode?: boolean;
  keyboardMove?: any;
}

type RedirectTo = string | { url: string, cookie: Cookie };

interface UserAutocompleteOpts {
  tag?: 'a' | 'span';
  minLength?: number;
  onSelect?: (value: string | { id: string; name: string }) => void;
  focus?: boolean;
  friend?: boolean;
  tour?: string;
  swiss?: string;
}

interface SoundBoxI {
  loadOggOrMp3(name: string, path: string): void;
  play(name: string): void;
  getVolume(): number;
  setVolume(v: number): void;
}

interface LichessSpeech {
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
  version?: string;
}

type Timeout = ReturnType<typeof setTimeout>;

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

interface LichessStorageHelper {
  make(k: string): LichessStorage;
  makeBoolean(k: string): LichessBooleanStorage;
  get(k: string): string | null;
  set(k: string, v: string): void;
  fire(k: string, v?: string): void;
  remove(k: string): void;
}

interface LichessStorage {
  get(): string | null;
  set(v: any): void;
  remove(): void;
  listen(f: (e: LichessStorageEvent) => void): void;
  fire(v?: string): void;
}

interface LichessBooleanStorage {
  get(): boolean;
  set(v: boolean): void;
  toggle(): void;
}

interface LichessStorageEvent {
  sri: string;
  nonce: number;
  value?: string;
}

interface LichessAnnouncement {
  msg?: string;
  date?: string;
}

interface Window {
  lichess: Lichess

  moment: any
  Mousetrap: any
  Chessground: any
  Highcharts: any
  lichessReplayMusic: () => {
    jump(node: Tree.Node): void
  }
  hopscotch: any;
  LichessSpeech?: LichessSpeech;
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

declare var SharedArrayBuffer: any | undefined;
declare var Atomics: any | undefined;

interface Navigator {
  deviceMemory: number;
}

declare type VariantKey = 'standard' | 'chess960' | 'antichess' | 'fromPosition' | 'kingOfTheHill' | 'threeCheck' | 'atomic' | 'horde' | 'racingKings' | 'crazyhouse';

declare type Speed = 'bullet' | 'blitz' | 'classical' | 'correspondence' | 'unlimited';

declare type Perf = 'bullet' | 'blitz' | 'classical' | 'correspondence' | 'chess960' | 'antichess' | 'fromPosition' | 'kingOfTheHill' | 'threeCheck' | 'atomic' | 'horde' | 'racingKings' | 'crazyhouse';

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
    puzzle?: 'win' | 'fail' | 'good' | 'retry';
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
  powerTip: any;
}

interface LichessModal {
  (html: string | JQuery, cls?: string, onClose?: () => void): JQuery;
  close(): void;
}

interface JQuery {
  powerTip(options?: PowerTip.Options | 'show' | 'hide'): JQuery;
  typeahead: any;
  sparkline: any;
  clock: any;
  highcharts(conf?: any): any;
  slider(key: string, value: any): any;
  slider(opts: any): any;
  flatpickr(opts: any): any;
  infinitescroll: any;
}

declare namespace PowerTip {
  type Placement = 'n' | 'e' | 's' | 'w' | 'nw' | 'ne' | 'sw' | 'se' | 'nw-alt' | 'ne-alt' | 'sw-alt' | 'se-alt';

  interface Options {
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
