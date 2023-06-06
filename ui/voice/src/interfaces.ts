import { Toggle } from 'common';
import { VNode } from 'snabbdom';

export interface VoiceCtrl {
  lang: (lang?: string) => string;
  micId: (deviceId?: string) => string;
  enabled: (enabled?: boolean) => boolean;
  toggle: () => void;
  showHelp: (v?: boolean) => boolean;
  pushTalk: (v?: boolean) => boolean;
  showPrefs: Toggle;
  module: () => VoiceModule | undefined;
  moduleId: string;
}

export interface VoiceModule {
  ui: VoiceCtrl;
  initGrammar: (recId?: string) => Promise<void>;
  prefNodes: (redraw?: () => void) => VNode[];
  allPhrases: () => [string, string][];
}

export interface RecognizerOpts {
  words: string[];
  recId: string;
  partial: boolean;
  audioCtx: AudioContext;
  broadcast: (text: string, msgType: Voice.MsgType, forMs: number) => void;
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
