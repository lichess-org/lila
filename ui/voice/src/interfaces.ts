import { Api as CgApi } from 'chessground/api';
import * as cg from 'chessground/types';
import { Prop } from 'common';

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
  allPhrases(): [string, string][];
  clarityPref(clarity?: number): number;
  timerPref(duration?: number): number;
  colorsPref(enabled?: boolean): boolean;
  update(fen: string, yourMove?: boolean): void;
  showHelp: Prop<boolean>;
  opponentRequest(request: string, callback: (v: boolean) => void): void;
  root: RootCtrl;
  debug?: any;
}

export interface RecognizerOpts {
  vocab: string[];
  mode: Voice.ListenMode;
  audioCtx: AudioContext;
  broadcast: (text: string, msgType: Voice.MsgType, words: Voice.WordResult | undefined, forMs: number) => void;
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
