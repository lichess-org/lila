import type { Shogiground } from 'shogiground';
import type { Howl, Howler } from 'howler';
import type Tagify from 'yaireo__tagify';
import type { Chart } from 'chart.js/auto';
import type flatpickr from 'flatpickr';
import type { LishogiModules } from './modules';
import type { InfiniteScroll } from './infinite-scroll';

declare global {
  interface Window {
    lishogi: {
      sri: string;

      // set on the server
      ready: Promise<void>;

      // file://./../../site/src/storage.ts
      storage: LishogiStorageHelper;
      tempStorage: LishogiStorageHelper;

      // file://./../../site/src/pubsub.ts
      pubsub: Pubsub;

      // file://./../../site/src/announce.ts
      announce(d: LishogiAnnouncement): void;

      // file://./../../site/src/sound.ts
      sound: SoundI;

      // file://./../../site/src/socket.ts
      socket: IStrongSocket;
      StrongSocket: {
        new (url: string, version: number | false, cfg?: any): IStrongSocket;
        firstConnect: Promise<(tpe: string, data: any) => void>;
        defaultParams: Record<string, any>;
      };

      // file://./../../site/src/navigation.ts
      redirect(o: string | { url: string; cookie: Cookie }): void;
      reload(): void;

      // file://./../../site/src/xhr.ts
      xhr: {
        json: LishogiXhrFetch<any>;
        text: LishogiXhrFetch<string>;
        formToXhr: (el: HTMLFormElement, submitter?: HTMLButtonElement) => Promise<Response>;
        urlWithParams: (
          base: string,
          params: Record<string, string | boolean | undefined | null | number>,
        ) => string;
      };

      // file://./../../site/src/timeago.ts
      timeago: {
        render(nodes: HTMLElement | HTMLElement[]): void;
        format(date: number | Date): string;
        absolute(date: number | Date): string;
      };

      // file://./../../site/src/challenge.ts
      challengeApp?: {
        update: (d: any) => void;
        open: () => void;
      };

      // file://./../../site/src/notify.ts
      notifyApp?: {
        update: (d: any, v: boolean) => void;
        setMsgRead(user: string): void;
      };

      // file://./../../site/src/infinite-scroll.ts
      loadInfiniteScroll: (sel: HTMLElement | string) => void;

      modules: LishogiModules;
      registerModule: (name: string, func: (...args: any[]) => any) => void;
      // initiated on the server
      modulesData: Record<string, any>;

      // flags
      properReload?: boolean;
      quietMode?: boolean;

      userAutocomplete: ($input: JQuery, opts: any) => Promise<any>;
      powertip: any;
      mousetrap: any;
    };

    Shogiground: typeof Shogiground;
    flatpickr: typeof flatpickr;

    Sortable: any;
    Howl: Howl;
    Howler: typeof Howler;
    Tagify: Tagify;
    Chart: typeof Chart;
    InfiniteScroll: typeof InfiniteScroll;
  }

  // handled by esbuild
  const __bundlename__: string;

  interface IStrongSocket {
    // Constructor parameters
    url: string;
    version: number | false;
    settings: Socket.Settings;

    // Key methods
    connect(): void;
    send: Socket.Send;
    disconnect(): void;
    destroy(): void;
    sign(s: string): void;

    // Getters and utility methods
    baseUrl(): string;
    getVersion(): number | false;
    pingInterval(): number;

    // Properties
    ws?: WebSocket;
    isOpen: boolean;
    options: Socket.Options;
    averageLag: number;
    nbConnects: number;
    autoReconnect: boolean;
  }

  interface SoundI {
    play: (name: string, volume?: number) => void;
    throttlePlay: (name: string, delay?: number, volume?: number) => () => void;
    setVolume: (value: number) => void;
    getVolume: () => number;
    enabled: () => boolean;
    speech: (v?: boolean) => boolean;
    say: (texts: { en?: string; jp?: string }, cut?: boolean, force?: boolean) => boolean;
    sayOrPlay: (name: string, texts: { en?: string; jp?: string }) => boolean;
    publish: () => void;
    changeSet: (s: string) => void;
    set: () => string;
    loadStandard: (name: string) => void;
    warmup: () => void;
  }

  interface LishogiSpeech {
    notation: string | undefined;
    cut: boolean;
  }

  type LishogiNvui = (redraw: () => void) => {
    render(ctrl: any): any;
  };

  interface LishogiAnnouncement {
    msg?: string;
    date?: string;
  }

  interface LishogiStorageEvent {
    sri: string;
    nonce: number;
    value?: string;
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
    set(v: boolean): void;
    toggle(): void;
  }

  interface LishogiStorageHelper {
    make(k: string): LishogiStorage;
    makeBoolean(k: string): LishogiBooleanStorage;
    get(k: string): string | null;
    set(k: string, v: string): void;
    fire(k: string, v?: string): void;
    remove(k: string): void;
  }

  type LishogiFetchGetContent = {
    url: Record<string, any>;
    json?: never;
    formData?: never;
  };
  type LishogiFetchContent =
    | {
        url?: Record<string, any>;
        json: Record<string, any>;
        formData?: never;
      }
    | {
        url?: Record<string, any>;
        formData: Record<string, any>;
        json?: never;
      }
    | LishogiFetchGetContent;

  type LishogiFetchInit = {
    cache?: RequestCache;
    signal?: AbortSignal;
  };

  type LishogiXhrFetch<T = any> = ((
    method: 'GET',
    url: string,
    content?: LishogiFetchGetContent,
    init?: LishogiFetchInit,
  ) => Promise<T>) &
    ((
      method: 'POST' | 'DELETE',
      url: string,
      content?: LishogiFetchContent,
      init?: LishogiFetchInit,
    ) => Promise<T>);

  type PubsubCallback = (...data: any[]) => void;

  interface Pubsub {
    on(msg: string, f: PubsubCallback): void;
    off(msg: string, f: PubsubCallback): void;
    emit(msg: string, ...args: any[]): void;
  }
}
