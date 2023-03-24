import { Api as CgApi } from 'chessground/api';
import * as cg from 'chessground/types';
import { Prop } from 'common';

export type MoveHandler = (fen: Fen, api: CgApi, yourMove?: boolean) => void;

export interface InputOpts {
  input?: HTMLInputElement;
  ctrl: VoiceMove;
}

export interface SubmitOpts {
  isTrusted: boolean;
  force?: boolean;
  yourMove?: boolean;
}

export type Submit = (v: string, submitOpts: SubmitOpts) => void;

export interface VoiceMove {
  available(): [string, string][];
  arrogance(conf?: number): number;
  arrowColors(enabled?: boolean): boolean;
  update(step: { fen: string }, yourMove?: boolean): void;
  modalOpen: Prop<boolean>;
  root: RootCtrl;
}

export interface CrazyPocket {
  [role: string]: number;
}
export interface RootData {
  crazyhouse?: { pockets: [CrazyPocket, CrazyPocket] };
  game: { variant: { key: VariantKey } };
  player: { color: Color };
}

export interface ClockCtrl {
  millisOf: (color: Color) => number;
}

export interface RootCtrl {
  chessground: CgApi;
  clock?: ClockCtrl;
  crazyValid?: (role: cg.Role, key: cg.Key) => boolean;
  data: RootData;
  offerDraw?: (v: boolean, immediately?: boolean) => void;
  takebackYes?: () => void;
  resign?: (v: boolean, immediately?: boolean) => void;
  rematch?: (accept?: boolean) => boolean;
  sendMove: (orig: cg.Key, dest: cg.Key, prom: cg.Role | undefined, meta?: cg.MoveMetadata) => void;
  sendNewPiece?: (role: cg.Role, key: cg.Key, isPredrop: boolean) => void;
  submitMove?: (v: boolean) => void;
  userJumpPlyDelta?: (plyDelta: Ply) => void;
  redraw: () => void;
  next?: () => void;
  vote?: (v: boolean) => void;
}

export interface KaldiOpts {
  keys: string[];
  audioCtx: AudioContext;
  broadcast: (msgText: string, msgType: Voice.MsgType, words: Voice.WordResult | undefined, forMs: number) => void;
  impl: 'vanilla' | 'worklet';
}
