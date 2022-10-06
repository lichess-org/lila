interface Lichess {
  load: Promise<void>; // DOMContentLoaded promise
  info: any;
  requestIdleCallback(f: () => void, timeout?: number): void;
  sri: string;
  storage: LichessStorageHelper;
  tempStorage: LichessStorageHelper;
  once(key: string, mod?: 'always'): boolean;
  powertip: LichessPowertip;
  clockWidget(el: HTMLElement, opts: { time: number; pause?: boolean }): void;
  spinnerHtml: string;
  assetUrl(url: string, opts?: AssetUrlOpts): string;
  loadCss(path: string): void;
  loadCssPath(path: string): Promise<void>;
  jsModule(name: string): string;
  loadScript(url: string, opts?: AssetUrlOpts): Promise<void>;
  loadModule(name: string): Promise<void>;
  loadIife(name: string, iife: keyof Window): Promise<any>;
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

  siteI18n: I18nDict;
  trans(i18n: I18nDict): Trans;
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
    update(node: HTMLElement, data: GameUpdate): void;
    finish(node: HTMLElement, win?: Color): void;
  };
  ab?: any;

  // socket.js
  StrongSocket: {
    new (url: string, version: number | false, cfg?: any): any;
    firstConnect: Promise<(tpe: string, data: any) => void>;
    defaultParams: Record<string, any>;
  };

  timeago(date: number | Date): string;
  dateFormat: () => (date: Date) => string;

  // misc
  advantageChart?: {
    update(data: any, mainline: any[]): void;
    (data: any, mainline: any[], trans: Trans, el: HTMLElement): void;
  };
  playMusic(): any;
  quietMode?: boolean;
  analysis?: any; // expose the analysis ctrl
}

type I18nDict = { [key: string]: string };
type I18nKey = string;

type RedirectTo = string | { url: string; cookie: Cookie };

type UserComplete = (opts: UserCompleteOpts) => void;

interface LichessPowertip {
  watchMouse(): void;
  manualGameIn(parent: HTMLElement): void;
  manualGame(el: HTMLElement): void;
  manualUser(el: HTMLElement): void;
  manualUserIn(parent: HTMLElement): void;
}

interface UserCompleteOpts {
  input: HTMLInputElement;
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
  loadOggOrMp3(name: string, path: string, noSoundSet?: boolean): void;
  loadStandard(name: string, soundSet?: string): void;
  play(name: string, volume?: number): void;
  playOnce(name: string): void;
  getVolume(): number;
  setVolume(v: number): void;
  speech(v?: boolean): boolean;
  changeSet(s: string): void;
  say(text: string, cut?: boolean, force?: boolean, translated?: boolean): boolean;
  sayOrPlay(name: string, text: string): void;
  preloadBoardSounds(): void;
  soundSet: string;
  baseUrl: string;
}

interface LichessSpeech {
  step(s: { san?: San }, cut: boolean): void;
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
  pluralSame(key: string, count: number, ...args: Array<string | number>): string;
  vdom<T>(key: string, ...args: T[]): Array<string | T>;
  vdomPlural<T>(key: string, count: number, countArg: T, ...args: T[]): Array<string | T>;
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

interface LichessEditor {
  getFen(): string;
  setOrientation(o: Color): void;
}

declare namespace Editor {
  export interface Config {
    baseUrl: string;
    fen?: string;
    options?: Editor.Options;
    is3d: boolean;
    animation: {
      duration: number;
    };
    embed: boolean;
    positions?: OpeningPosition[];
    endgamePositions?: EndgamePosition[];
    i18n: I18nDict;
  }

  export interface Options {
    orientation?: Color;
    onChange?: (fen: string) => void;
    inlineCastling?: boolean;
  }

  export interface OpeningPosition {
    eco?: string;
    name: string;
    fen: string;
    epd?: string;
  }

  export interface EndgamePosition {
    name: string;
    fen: string;
    epd?: string;
  }
}

type Nvui = (redraw: () => void) => {
  render(ctrl: any): any;
};

interface Window {
  lichess: Lichess;

  readonly chrome?: unknown;
  readonly moment: any;
  readonly Mousetrap: any;
  Chessground: any;
  readonly InfiniteScroll: (selector: string) => void;
  readonly lichessReplayMusic: () => {
    jump(node: Tree.Node): void;
  };
  readonly hopscotch: any;
  LichessSpeech?: LichessSpeech;
  readonly LichessEditor?: (element: HTMLElement, config: Editor.Config) => LichessEditor;
  LichessChat: (element: Element, opts: any) => any;
  readonly LichessFlatpickr: (element: Element, opts: any) => any;
  readonly LichessNotify: (element: any, opts: any) => any;
  readonly LichessChallenge: (element: any, opts: any) => any;
  readonly LichessDasher: (element: any) => any;
  readonly LichessAnalyse: any;
  readonly LichessCli: any;
  readonly LichessRound: any;
  readonly LichessRoundNvui?: Nvui;
  readonly LichessPuzzleNvui?: Nvui;
  readonly LichessAnalyseNvui?: (ctrl: any) => {
    render(): any;
  };
  readonly LichessChartRatingHistory?: any;
  readonly LichessKeyboardMove?: any;
  readonly stripeHandler: any;
  readonly Stripe: any;
  readonly Textcomplete: any;
  readonly UserComplete: any;
  readonly Sortable: any;
  readonly Peer: any;
  readonly Highcharts: any;
  readonly LilaLpv: {
    autostart(): void;
    loadPgnAndStart(el: HTMLElement, url: string, opts: any): Promise<void>;
  };

  readonly Palantir: unknown;
  readonly passwordComplexity: unknown;
  readonly Tagify: unknown;
  readonly paypalOrder: unknown;
  readonly paypalSubscription: unknown;
}

interface Study {
  userId?: string | null;
  isContrib?: boolean;
  isOwner?: boolean;
  closeActionMenu?(): void;
  setTab(tab: string): void;
}

interface LightUserNoId {
  name: string;
  title?: string;
  patron?: boolean;
}

interface LightUser extends LightUserNoId {
  id: string;
}

interface LightUserOnline extends LightUser {
  online: boolean;
}

interface Navigator {
  deviceMemory?: number; // https://developer.mozilla.org/en-US/docs/Web/API/Navigator/deviceMemory
}

declare type VariantKey =
  | 'standard'
  | 'chess960'
  | 'antichess'
  | 'fromPosition'
  | 'kingOfTheHill'
  | 'threeCheck'
  | 'atomic'
  | 'horde'
  | 'racingKings'
  | 'crazyhouse';

declare type Speed = 'ultraBullet' | 'bullet' | 'blitz' | 'rapid' | 'classical' | 'correspondence';

declare type Perf =
  | 'ultraBullet'
  | 'bullet'
  | 'blitz'
  | 'rapid'
  | 'classical'
  | 'correspondence'
  | 'chess960'
  | 'antichess'
  | 'fromPosition'
  | 'kingOfTheHill'
  | 'threeCheck'
  | 'atomic'
  | 'horde'
  | 'racingKings'
  | 'crazyhouse';

declare type Color = 'white' | 'black';

declare type Files = 'a' | 'b' | 'c' | 'd' | 'e' | 'f' | 'g' | 'h';
declare type Ranks = '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8';
declare type Key = 'a0' | `${Files}${Ranks}`;
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
  currentPageResults: A[];
  nbResults: number;
  previousPage?: number;
  nextPage?: number;
  nbPages: number;
}

declare namespace Tree {
  export type Path = string;

  interface ClientEvalBase {
    fen: Fen;
    depth: number;
    nodes: number;
    pvs: PvData[];
    cp?: number;
    mate?: number;
  }
  export interface CloudEval extends ClientEvalBase {
    cloud: true;
    maxDepth: undefined;
    millis: undefined;
  }
  export interface LocalEval extends ClientEvalBase {
    cloud?: false;
    maxDepth: number;
    knps: number;
    millis: number;
  }
  export type ClientEval = CloudEval | LocalEval;

  export interface ServerEval {
    cp?: number;
    mate?: number;
    best?: Uci;
    fen: Fen;
    knodes: number;
    depth: number;
    pvs: PvDataServer[];
  }

  export interface PvDataServer {
    moves: string;
    mate?: number;
    cp?: number;
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
    threat?: LocalEval;
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

interface GameUpdate {
  id: string;
  fen: Fen;
  lm: Uci;
  wc?: number;
  bc?: number;
}

interface CashStatic {
  powerTip: any;
}

interface Cash {
  powerTip(options?: PowerTip.Options | 'show' | 'hide'): Cash;
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

declare namespace Prefs {
  const enum Coords {
    Hidden = 0,
    Inside = 1,
    Outside = 2,
  }

  const enum AutoQueen {
    Never = 1,
    OnPremove = 2,
    Always = 3,
  }

  const enum ShowClockTenths {
    Never = 0,
    Below10Secs = 1,
    Always = 2,
  }

  const enum ShowResizeHandle {
    Never = 0,
    OnlyAtStart = 1,
    Always = 2,
  }

  const enum MoveEvent {
    Click = 0,
    Drag = 1,
    ClickOrDrag = 2,
  }

  const enum Replay {
    Never = 0,
    OnlySlowGames = 1,
    Always = 2,
  }
}

interface Dictionary<T> {
  [key: string]: T | undefined;
}

type SocketHandlers = Dictionary<(d: any) => void>;

declare const lichess: Lichess;
