interface Lishogi {
  // standalones/util.js
  requestIdleCallback(f: () => void, timeout?: number): void;
  dispatchEvent(el: HTMLElement | Window, eventName: string): void;
  hasTouchEvents: boolean;
  sri: string;
  isCol1(): boolean;
  storage: LishogiStorageHelper;
  tempStorage: LishogiStorageHelper; // TODO: unused
  once(key: string, mod?: 'always'): boolean;
  debounce(func: (...args: any[]) => void, wait: number, immediate?: boolean): (...args: any[]) => void;
  powertip: any;
  widget: unknown;
  hoverable?: boolean;
  isHoverable(): boolean;
  spinnerHtml: string;
  assetUrl(url: string, opts?: AssetUrlOpts): string;
  loadedCss: { [key: string]: boolean };
  loadCss(path: string): void;
  loadCssPath(path: string): void;
  loadChushogiPieceSprite(): void;
  loadKyotoshogiPieceSprite(): void;
  compiledScript(path: string): string;
  loadScript(url: string, opts?: AssetUrlOpts): Promise<unknown>;
  hopscotch: any;
  slider(): any;
  spectrum: any;
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
  parseSfen(el: any): void;
  challengeApp: any;
  ab?: any;

  // socket.js
  StrongSocket: {
    (url: string, version: number | false, cfg: any): any;
    defaults: {
      events: {
        sfen(e: any): void;
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
  advantageChart?: {
    update(data: any): void;
    (data: any, trans: Trans, el: HTMLElement): void;
  };
  movetimeChart: any;
  RoundNVUI?(redraw: () => void): {
    render(ctrl: any): any;
  };
  AnalyseNVUI?(redraw: () => void): {
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
  notation(notation: string | undefined, cut: boolean): void;
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

declare type SocketSend = (type: string, data?: any, opts?: any, noRetry?: boolean) => void;

type I18nKey = import('./i18n').I18nKey;

type TransNoArg = (key: I18nKey) => string;

interface Trans {
  (key: I18nKey, ...args: Array<string | number>): string;
  noarg: TransNoArg;
  noargOrCapitalize: (key: I18nKey | string) => string;
  plural(key: I18nKey, count: number, ...args: Array<string | number>): string;
  vdom<T>(key: I18nKey, ...args: T[]): (string | T)[];
  vdomPlural<T>(key: I18nKey, count: number, countArg: T, ...args: T[]): (string | T)[];
}

type PubsubCallback = (...data: any[]) => void;

type Timeout = ReturnType<typeof setTimeout>;

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

declare type VariantKey = 'standard' | 'minishogi' | 'chushogi' | 'annanshogi' | 'kyotoshogi' | 'checkshogi';

declare type Speed = 'ultraBullet' | 'bullet' | 'blitz' | 'rapid' | 'classical' | 'correspondence' | 'unlimited';

declare type Perf =
  | 'ultraBullet'
  | 'bullet'
  | 'blitz'
  | 'rapid'
  | 'classical'
  | 'correspondence'
  | 'minishogi'
  | 'chushogi'
  | 'annanshogi'
  | 'kyotoshogi'
  | 'checkshogi';

declare type Color = 'sente' | 'gote';

declare type Files =
  | '1'
  | '2'
  | '3'
  | '4'
  | '5'
  | '6'
  | '7'
  | '8'
  | '9'
  | '10'
  | '11'
  | '12'
  | '13'
  | '14'
  | '15'
  | '16';
declare type Ranks = 'a' | 'b' | 'c' | 'd' | 'e' | 'f' | 'g' | 'h' | 'i' | 'j' | 'k' | 'l' | 'm' | 'n' | 'o' | 'p';
declare type Key = `${Files}${Ranks}`;

declare type MoveNotation = string;
declare type Usi = string;
declare type Sfen = string;
declare type Ply = number;

interface Variant {
  key: VariantKey;
  name: string;
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

  interface EvalScore {
    cp?: number;
    mate?: number;
  }

  interface ClientEvalBase extends EvalScore {
    sfen: Sfen;
    depth: number;
    nodes: number;
    pvs: PvData[];
  }
  export interface CloudEval extends ClientEvalBase {
    cloud: true;
    maxDepth: undefined;
    millis: undefined;
  }
  export interface LocalEval extends ClientEvalBase {
    cloud?: false;
    enteringKingRule: boolean;
    maxDepth: number;
    knps: number;
    millis: number;
  }
  export type ClientEval = CloudEval | LocalEval;

  export interface ServerEval extends EvalScore {
    best?: Usi;
    sfen: Sfen;
    path: string;
    knodes: number;
    depth: number;
    pvs: PvDataServer[];
  }

  export interface PvData {
    moves: string[];
    mate?: number;
    cp?: number;
  }

  export interface PvDataServer extends EvalScore {
    moves: string;
  }

  export interface TablebaseHit {
    winner: Color | undefined;
    best?: Usi;
  }

  export interface Node {
    id: string;
    ply: Ply;
    usi?: Usi;
    notation?: string;
    sfen: Sfen;
    children: Node[];
    comments?: Comment[];
    gamebook?: Gamebook;
    check?: boolean;
    capture?: boolean;
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
    fourfold?: boolean;
    fail?: boolean;
    puzzle?: 'win' | 'fail' | 'good';
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
  (html: string | JQuery, cls?: string, onClose?: () => void, withDataAndEvents?: boolean): JQuery;
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
  slider(key: string, value: any): any;
  slider(opts: any): any;
  spectrum(opts: any): any;
  flatpickr(opts: any): any;
  multipleSelect(opts: any): any;
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
