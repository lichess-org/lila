interface Lichess {
  load: Promise<unknown>; // window.onload promise
  info: any;
  requestIdleCallback(f: () => void, timeout?: number): void;
  sri: string;
  storage: LichessStorageHelper;
  tempStorage: LichessStorageHelper;
  once(key: string, mod?: 'always'): boolean;
  powertip: any;
  widget: any;
  spinnerHtml: string;
  assetUrl(url: string, opts?: AssetUrlOpts): string;
  loadCss(path: string): void;
  loadCssPath(path: string): void;
  jsModule(name: string): string;
  loadScript(url: string, opts?: AssetUrlOpts): Promise<void>;
  loadModule(name: string): Promise<void>;
  hopscotch: any;
  userComplete: () => Promise<UserComplete>;
  slider(): Promise<void>;
  makeChat(data: any): any;
  idleTimer(delay: number, onIdle: () => void, onWakeUp: () => void): void;
  pubsub: Pubsub;
  contentLoaded(parent?: HTMLElement): void;
  unload: {
    expected: boolean;
  };
  watchers(el: HTMLElement): void;
  redirect(o: RedirectTo): void;
  reload(): void;
  escapeHtml(str: string): string;
  announce(d: LichessAnnouncement): void;
  studyTour(study: Study): void;
  studyTourChapter(study: Study): void;

  trans(i18n: { [key: string]: string | undefined }): Trans
  quantity(n: number): 'zero' | 'one' | 'few' | 'many' | 'other';

  socket: any;
  sound: SoundI;
  miniBoard: {
    init(node: HTMLElement): void;
    initAll(parent?: HTMLElement): void;
  };
  miniGame: {
    init(node: HTMLElement): string | null;
    initAll(parent?: HTMLElement): void;
    update(node: HTMLElement, data: { fen: string, lm: string, wc?: number, bc?: number }): void;
    finish(node: HTMLElement, win?: Color): void;
  };
  ab?: any;

  // socket.js
  StrongSocket: {
    new(url: string, version: number | false, cfg?: any): any;
    firstConnect: Promise<(tpe: string, data: any) => void>
    defaultParams: Record<string, any>;
  }

  timeago(date: number | Date): string;
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

type UserComplete = (opts: UserCompleteOpts) => void;

interface UserCompleteOpts {
  input: HTMLInputElement,
  tag?: 'a' | 'span';
  minLength?: number;
  populate?: (result: LightUser) => string;
  onSelect?: (result: LightUser) => void;
  focus?: boolean;
  friend?: boolean;
  tour?: string;
  swiss?: string;
}

interface SoundI {
  loadOggOrMp3(name: string, path: string): void;
  loadStandard(name: string, soundSet?: string): void;
  play(name: string, volume?: number): void;
  getVolume(): number;
  setVolume(v: number): void;
  speech(v?: boolean): boolean;
  changeSet(s: string): void;
  say(text: any, cut?: boolean, force?: boolean): boolean;
  sayOrPlay(name: string, text: string): void;
  soundSet: string;
  baseUrl: string;
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
  InfiniteScroll(selector: string): void;
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

interface Study{
  userId?: string | null;
  isContrib?: boolean;
  isOwner?: boolean;
  setTab(tab: string): void;
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
    fen: Fen;
    knodes: number;
    depth: number;
    pvs: PvData[];
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

interface CashStatic {
  powerTip: any;
}

interface Cash {
  powerTip(options?: PowerTip.Options | 'show' | 'hide'): Cash;
  clock: any;
}

declare namespace PowerTip {
  type Placement = 'n' | 'e' | 's' | 'w' | 'nw' | 'ne' | 'sw' | 'se' | 'nw-alt' | 'ne-alt' | 'sw-alt' | 'se-alt';

  interface Options {
    preRender?: (el: HTMLElement) => void;
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

declare module '@yaireo/tagify';

declare var lichess: Lichess;
