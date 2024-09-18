/// <reference path="./tree.d.ts" />
/// <reference path="./chessground.d.ts" />
/// <reference path="./cash.d.ts" />

// file://./../../site/src/site.ts
interface Site {
  debug: boolean;
  info: {
    commit: string;
    message: string;
    date: string;
  };
  mousetrap: LichessMousetrap; // file://./../../site/src/mousetrap.ts
  sri: string;
  powertip: LichessPowertip; // file://./../../site/src/powertip.ts
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
  };
  unload: { expected: boolean };
  redirect(o: RedirectTo, beep?: boolean): void;
  reload(err?: any): void;
  announce(d: LichessAnnouncement): void;
  trans: Trans; // file://./../../common/src/i18n.ts
  sound: SoundI; // file://./../../site/src/sound.ts
  displayLocale: string; // file://./../../common/src/i18n.ts
  blindMode: boolean;

  // the following are not set in site.ts
  load: Promise<void>; // DOMContentLoaded promise
  quantity(n: number): 'zero' | 'one' | 'few' | 'many' | 'other';
  siteI18n: I18nDict;
  socket: SocketI;
  quietMode?: boolean;
  analysis?: any; // expose the analysis ctrl
  manifest: { css: Record<string, string>; js: Record<string, string>; hashed: Record<string, string> };
}

interface EsmModuleOpts extends AssetUrlOpts {
  init?: any;
  npm?: boolean;
}

type PairOf<T> = [T, T];

type I18nDict = { [key: string]: string };
type I18nKey = string;

type Flair = string;

type RedirectTo = string | { url: string; cookie: Cookie };

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

type SoundMoveOpts = {
  name?: string; // either provide this or valid san/uci
  san?: string;
  uci?: string;
  volume?: number;
  filter?: 'music' | 'game';
};

type SoundMove = (opts?: SoundMoveOpts) => void;

type SoundListener = (event: 'start' | 'stop', text?: string) => void;

interface SoundI {
  // file://./../../site/src/sound.ts
  listeners: Set<SoundListener>;
  theme: string;
  move: SoundMove;
  load(name: string, path?: string): Promise<any>;
  play(name: string, volume?: number): Promise<void>;
  playOnce(name: string): void;
  countdown(count: number, intervalMs?: number): Promise<void>;
  getVolume(): number;
  setVolume(v: number): void;
  speech(v?: boolean): boolean;
  changeSet(s: string): void;
  say(text: string, cut?: boolean, force?: boolean, translated?: boolean): boolean;
  saySan(san?: San, cut?: boolean): void;
  sayOrPlay(name: string, text: string): void;
  preloadBoardSounds(): void;
  url(name: string): string;
}

interface SocketI {
  averageLag: number;
  pingInterval(): number;
  getVersion(): number|false;
  send: SocketSend;
  sign(s: string): void;
  destroy(): void;
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
  // file://./../../common/src/i18n.ts
  (key: string, ...args: Array<string | number>): string;
  noarg: TransNoArg;
  plural(key: string, count: number, ...args: Array<string | number>): string;
  pluralSame(key: string, count: number, ...args: Array<string | number>): string;
  vdom<T>(key: string, ...args: T[]): Array<string | T>;
  vdomPlural<T>(key: string, count: number, countArg: T, ...args: T[]): Array<string | T>;
}

interface LichessAnnouncement {
  msg?: string;
  date?: string;
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

declare type Perf = Exclude<VariantKey, 'standard'> | Speed;

declare type Color = 'white' | 'black';

declare type Files = 'a' | 'b' | 'c' | 'd' | 'e' | 'f' | 'g' | 'h';
declare type Ranks = '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8';
declare type Key = 'a0' | `${Files}${Ranks}`;
declare type Uci = string;
declare type San = string;
declare type Ply = number;
declare type Seconds = number;
declare type Centis = number;
declare type Millis = number;

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
declare module 'tablesort';
