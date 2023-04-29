import { Api as CgApi } from 'chessground/api';
import * as cg from 'chessground/types';
import { Prop, Toggle } from 'common';

export interface RootCtrl {
  chessground: CgApi;
  sendMove: (orig: cg.Key, dest: cg.Key, prom: cg.Role | undefined, meta?: cg.MoveMetadata) => void;
  redraw: () => void;
  flipNow: () => void;
  offerDraw?: (v: boolean, immediately?: boolean) => void;
  takebackYes?: () => void;
  resign?: (v: boolean, immediately?: boolean) => void;
  rematch?: (accept?: boolean) => boolean;
  next?: () => void;
  vote?: (v: boolean) => void;
  solve?: () => void;
}

export interface VoiceMove {
  showHelp: Prop<boolean>;
  showSettings: Toggle;
  clarityPref: Prop<number>;
  timerPref: Prop<number>;
  colorsPref: Prop<boolean>;
  langPref: Prop<string>;
  allPhrases: () => [string, string][];
  update(fen: string, yourMove?: boolean): void;
  opponentRequest(request: string, callback: (v: boolean) => void): void;
}

export interface RecognizerOpts {
  vocab: string[];
  mode: Voice.ListenMode;
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

export type Partials = {
  commands: string[];
  colors: string[];
  numbers: string[];
};

export type Grammar = {
  partials: Partials;
  entries: Entry[];
};
