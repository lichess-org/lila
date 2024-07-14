// eslint-disable-next-line
/// <reference path="./tree.d.ts" />
// eslint-disable-next-line
/// <reference path="./chessground.d.ts" />
// eslint-disable-next-line
/// <reference path="./voice.d.ts" />
// eslint-disable-next-line
/// <reference path="./cash.d.ts" />

// file://./../../site/src/site.ts
interface Site {
  debug: boolean;
  info: {
    commit: string;
    message: string;
    date: string;
  };
  StrongSocket: {
    // file://./../../site/src/socket.ts
    new (url: string, version: number | false, cfg?: any): any;
    firstConnect: Promise<(tpe: string, data: any) => void>;
    defaultParams: Record<string, any>;
  };
  mousetrap: LichessMousetrap; // file://./../../site/src/mousetrap.ts
  sri: string;
  storage: LichessStorageHelper;
  tempStorage: LichessStorageHelper;
  once(key: string, mod?: 'always'): boolean;
  powertip: LichessPowertip; // file://./../../site/src/powertip.ts
  clockWidget(el: HTMLElement, opts: { time: number; pause?: boolean }): void;
  spinnerHtml: string;
  asset: {
    // file://./../../site/src/asset.ts
    baseUrl(): string;
    url(url: string, opts?: AssetUrlOpts): string;
    flairSrc(flair: Flair): string;
    loadCss(href: string): Promise<void>;
    loadCssPath(key: string): Promise<void>;
    removeCss(href: string): void;
    removeCssPath(key: string): void;
    jsModule(name: string): string;
    loadIife(path: string, opts?: AssetUrlOpts): Promise<void>;
    loadEsm<T>(key: string, opts?: EsmModuleOpts): Promise<T>;
    userComplete(opts: UserCompleteOpts): Promise<UserComplete>;
  };
  idleTimer(delay: number, onIdle: () => void, onWakeUp: () => void): void;
  pubsub: Pubsub; // file://./../../site/src/pubsub.ts
  unload: { expected: boolean };
  redirect(o: RedirectTo, beep?: boolean): void;
  reload(): void;
  watchers(el: HTMLElement): void;
  announce(d: LichessAnnouncement): void;
  trans(i18n: I18nDict): Trans;
  sound: SoundI; // file://./../../site/src/sound.ts
  mic: Voice.Microphone; // file://./../../site/src/mic.ts
  miniBoard: {
    // file://./../../common/src/miniBoard.ts
    init(node: HTMLElement): void;
    initAll(parent?: HTMLElement): void;
  };
  miniGame: {
    // file://./../../site/src/miniGame.ts
    init(node: HTMLElement): string | null;
    initAll(parent?: HTMLElement): void;
    update(node: HTMLElement, data: MiniGameUpdateData): void;
    finish(node: HTMLElement, win?: Color): void;
  };
  timeago(date: number | Date): string;
  dateFormat: () => (date: Date) => string;
  displayLocale: string;
  contentLoaded(parent?: HTMLElement): void;
  blindMode: boolean;
  makeChat(data: any): any;
  makeChessground(el: HTMLElement, config: CgConfig): CgApi;
  log: LichessLog; // file://./../../site/src/log.ts

  // the remaining are not set in site.lichess.globals.ts
  load: Promise<void>; // DOMContentLoaded promise
  quantity(n: number): 'zero' | 'one' | 'few' | 'many' | 'other';
  siteI18n: I18nDict;
  socket: any;
  quietMode?: boolean;
  analysis?: any; // expose the analysis ctrl
  manifest: { css: Record<string, string>; js: Record<string, string>; hashed: Record<string, string> };
}

interface EsmModuleOpts extends AssetUrlOpts {
  init?: any;
  npm?: boolean;
}

interface LichessLog {
  (...args: any[]): Promise<void>;
  clear(): Promise<void>;
  get(): Promise<string>;
}

type PairOf<T> = [T, T];

type I18nDict = { [key: string]: string };
type I18nKey = string;

type Flair = string;

type RedirectTo = string | { url: string; cookie: Cookie };

type UserComplete = (opts: UserCompleteOpts) => void;

interface LichessMousetrap {
  // file://./../../site/src/mousetrap.ts
  bind(
    keys: string | string[],
    callback: (e: KeyboardEvent) => void,
    action?: 'keypress' | 'keydown' | 'keyup',
  ): LichessMousetrap;
}

interface LichessPowertip {
  // file://./../../site/src/powertip.ts
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
  team?: string;
}

interface QuestionChoice {
  // file://./../../round/src/ctrl.ts
  action: () => void;
  icon?: string;
  key?: I18nKey;
}

interface QuestionOpts {
  prompt: string; // TODO i18nkey, or just always pretranslate
  yes?: QuestionChoice;
  no?: QuestionChoice;
}

type SoundMove = (opts?: {
  // file://./../../site/src/sound.ts
  name?: string; // either provide this or valid san/uci
  san?: string;
  uci?: string;
  filter?: 'music' | 'game'; // undefined allows either
}) => void;

interface SoundI {
  // file://./../../site/src/sound.ts
  ctx?: AudioContext;
  load(name: string, path?: string): void;
  play(name: string, volume?: number): Promise<void>;
  playOnce(name: string): void;
  move: SoundMove;
  countdown(count: number, intervalMs?: number): Promise<void>;
  getVolume(): number;
  setVolume(v: number): void;
  speech(v?: boolean): boolean;
  changeSet(s: string): void;
  say(text: string, cut?: boolean, force?: boolean, translated?: boolean): boolean;
  saySan(san?: San, cut?: boolean): void;
  sayOrPlay(name: string, text: string): void;
  preloadBoardSounds(): void;
  theme: string;
  url(name: string): string;
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
  documentOrigin?: boolean;
  pathOnly?: boolean;
  version?: false | string;
}

type Timeout = ReturnType<typeof setTimeout>;

declare type SocketSend = (type: string, data?: any, opts?: any, noRetry?: boolean) => void;

type TransNoArg = (key: string) => string;

interface Trans {
  // file://./../../site/src/trans.ts
  (key: string, ...args: Array<string | number>): string;
  noarg: TransNoArg;
  plural(key: string, count: number, ...args: Array<string | number>): string;
  pluralSame(key: string, count: number, ...args: Array<string | number>): string;
  vdom<T>(key: string, ...args: T[]): Array<string | T>;
  vdomPlural<T>(key: string, count: number, countArg: T, ...args: T[]): Array<string | T>;
}

type PubsubCallback = (...data: any[]) => void;

interface Pubsub {
  // file://./../../site/src/pubsub.ts
  on(msg: string, f: PubsubCallback): void;
  off(msg: string, f: PubsubCallback): void;
  emit(msg: string, ...args: any[]): void;
}

interface LichessStorageHelper {
  make(k: string, ttl?: number): LichessStorage;
  boolean(k: string): LichessBooleanStorage;
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
  getOrDefault(defaultValue: boolean): boolean;
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
  getFen(): CgFEN;
  setOrientation(o: Color): void;
}

declare namespace Editor {
  // file://./../../editor/src/ctrl.ts
  export interface Config {
    el: HTMLElement;
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
    coordinates?: boolean;
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
  site: Site;
  $as<T>(cash: Cash): T;
  readonly chrome?: unknown;
  readonly moment: any;
  readonly stripeHandler: any;
  readonly Stripe: any;
  readonly Textcomplete: any;
  readonly UserComplete: any;
  readonly Tagify: unknown;
  readonly paypalOrder: unknown;
  readonly paypalSubscription: unknown;
  readonly webkitAudioContext?: typeof AudioContext;
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
  flair?: Flair;
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

interface EvalScore {
  cp?: number;
  mate?: number;
}

interface MiniGameUpdateData {
  fen: CgFEN;
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
  type Placement =
    | 'n'
    | 'e'
    | 's'
    | 'w'
    | 'nw'
    | 'ne'
    | 'sw'
    | 'se'
    | 'nw-alt'
    | 'ne-alt'
    | 'sw-alt'
    | 'se-alt';

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

interface Dictionary<T> {
  [key: string]: T | undefined;
}

type SocketHandlers = Dictionary<(d: any) => void>;

declare const site: Site;
declare const $as: <T>(cashOrHtml: Cash | string) => T;
declare module 'tablesort';
