import { Api as CgApi } from 'chessground/api';
import * as cg from 'chessground/types';
import { Prop } from 'common';

export interface RootCtrl {
  chessground: CgApi;
  offerDraw?: (v: boolean, immediately?: boolean) => void;
  takebackYes?: () => void;
  resign?: (v: boolean, immediately?: boolean) => void;
  rematch?: (accept?: boolean) => boolean;
  sendMove: (orig: cg.Key, dest: cg.Key, prom: cg.Role | undefined, meta?: cg.MoveMetadata) => void;
  redraw: () => void;
  next?: () => void;
  vote?: (v: boolean) => void;
  solve?: () => void;
}

export interface VoiceMove {
  allPhrases(): [string, string][];
  arrogance(conf?: number): number;
  countdown(countdown?: number): number;
  arrowColors(enabled?: boolean): boolean;
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
