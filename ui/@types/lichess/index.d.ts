/// <reference path="./tree.d.ts" />
/// <reference path="./chessground.d.ts" />
/// <reference path="./cash.d.ts" />
/// <reference path="./i18n.d.ts" />

// file://./../../site/src/site.ts
interface Site {
  debug: boolean | string;
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
  sound: SoundI; // file://./../../site/src/sound.ts
  displayLocale: string; // file://./../../common/src/i18n.ts
  blindMode: boolean;
  load: Promise<void>; // DOMContentLoaded promise
  quantity(n: number): 'zero' | 'one' | 'two' | 'few' | 'many' | 'other';
  quietMode?: boolean;
  analysis?: any; // expose the analysis ctrl
  // file://./../../.build/src/manifest.ts
  manifest: { css: Record<string, string>; js: Record<string, string>; hashed: Record<string, string> };
}

interface EsmModuleOpts extends AssetUrlOpts {
  init?: any;
  npm?: boolean;
}

type PairOf<T> = [T, T];

type Flair = string;
type Redraw = () => void;
type RedirectTo = string | { url: string; cookie: Cookie };

interface LichessMousetrap {
  // file://./../../site/src/mousetrap.ts
  unbind(key: string): void;
  bind(
    keys: string | string[],
    callback: (e: KeyboardEvent) => void,
    action?: 'keypress' | 'keydown' | 'keyup',
    multiple?: boolean, // = true
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
  text?: string;
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
  playAndDelayMateResultIfNecessary(name: string, volume?: number): Promise<void>;
  playOnce(name: string): void;
  countdown(count: number, intervalMs?: number): Promise<void>;
  getVolume(): number;
  setVolume(v: number): void;
  getVoice(): SpeechSynthesisVoice | undefined;
  getVoiceMap(): Map<string, SpeechSynthesisVoice>;
  setVoice(v: { name: string; lang: string }): void;
  speech(v?: boolean): boolean;
  changeSet(s: string): void;
  sayLazy(text: () => string, cut?: boolean, force?: boolean, translated?: boolean): boolean;
  say(text: string, cut?: boolean, force?: boolean, translated?: boolean): boolean;
  saySan(san?: San, cut?: boolean, force?: boolean): void;
  sayOrPlay(name: string, text: string): void;
  preloadBoardSounds(): void;
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
  pathVersion?: true | string;
}

interface Dictionary<T> {
  [key: string]: T | undefined;
}

type SocketHandlers = Dictionary<(d: any) => void>;

type Timeout = ReturnType<typeof setTimeout>;

type SocketSend = (type: string, data?: any, opts?: any, noRetry?: boolean) => void;

interface LichessAnnouncement {
  msg?: string;
  date?: string;
}

type Nvui = (redraw: () => void) => {
  render(ctrl: any): any;
};

interface Fipr {
  get(cb: (c: { value: string }[]) => void): void;
  x64hash128(input: string, seed: number): string;
}

interface Events {
  on(key: string, cb: (...args: any[]) => void): void;
  off(key: string, cb: (...args: any[]) => void): void;
}

interface Window {
  site: Site;
  fipr: Fipr;
  i18n: I18n;
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

type VariantKey =
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

type Speed = 'ultraBullet' | 'bullet' | 'blitz' | 'rapid' | 'classical' | 'correspondence';

type Perf = Exclude<VariantKey, 'standard'> | Speed;

type UserId = string;
type Uci = string;
type San = string;
type Ply = number;
type Seconds = number;
type Centis = number;
type Millis = number;

type ByColor<T> = { [C in Color]: T };

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

declare const site: Site;
declare const fipr: Fipr;
declare const i18n: I18n;
declare module 'tablesort';
declare const $html: (s: TemplateStringsArray, ...k: any[]) => string; // file://./../../.build/src/esbuild.ts
