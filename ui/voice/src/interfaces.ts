import type { Prop, Toggle } from 'lib';
import type { VNode } from 'snabbdom';

export type MsgType = 'full' | 'partial' | 'status' | 'error' | 'stop' | 'start';
export type ListenMode = 'full' | 'partial';
export type Listener = (msgText: string, msgType: MsgType) => void;

export interface Microphone {
  setLang(language: string): void;

  getMics(): Promise<MediaDeviceInfo[]>;
  setMic(micId: string): void;

  initRecognizer(
    words: string[],
    also?: {
      recId?: string; // = 'default' if not provided
      partial?: boolean; // = false
      listener?: Listener; // = undefined
      listenerId?: string; // = recId (specify for multiple listeners on same recId)
    },
  ): void;
  setRecognizer(recId: string): void;

  addListener(
    listener: Listener,
    also?: {
      recId?: string; // = 'default'
      listenerId?: string; // = recId
    },
  ): void;
  removeListener(listenerId: string): void;
  setController(listener: Listener): void; // for status display, indicators, etc
  stopPropagation(): void; // interrupt broadcast propagation on current rec (for modal interactions)

  start(listen?: boolean): Promise<void>; // listen = true if not provided, if false just initialize
  stop(): void; // stop listening/downloading/whatever

  readonly isListening: boolean;
  readonly isBusy: boolean; // are we downloading, extracting, or loading?
  readonly status: string; // status display for setController listener
  readonly recId: string; // get current recognizer
  readonly micId: string;
  readonly lang: string; // defaults to 'en'
}

export interface VoiceCtrl {
  mic: Microphone;
  lang: (lang?: string) => string;
  micId: (deviceId?: string) => string;
  enabled: (enabled?: boolean) => boolean;
  toggle: () => void;
  flash: () => void;
  showHelp: (v?: boolean | 'list') => boolean | 'list';
  pushTalk: Prop<boolean>;
  showPrefs: Toggle;
  module: () => VoiceModule | undefined;
  moduleId: string;
}

export interface VoiceModule {
  ctrl: VoiceCtrl;
  initGrammar: (recId?: string) => Promise<void>;
  prefNodes: (redraw?: () => void) => VNode[];
  allPhrases: () => [string, string][];
}

export interface VoskModule {
  initModel: (url: string, lang: string) => Promise<void>;
  initRecognizer: (opts: RecognizerOpts) => AudioNode | undefined;
  isLoaded: (lang?: string) => boolean;
  select: (recId: string | false) => void;
}

export interface RecognizerOpts {
  words: string[];
  recId: string;
  partial: boolean;
  audioCtx: AudioContext;
  broadcast: (text: string, msgType: MsgType, forMs: number) => void;
}

export type Sub = {
  to: string;
  cost: number;
};

export type Entry = {
  in: string;
  tok: string;
  tags: string[];
  val?: string;
  subs?: Sub[];
};
