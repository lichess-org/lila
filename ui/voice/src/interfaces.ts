import { Api as CgApi } from 'chessground/api';
import * as cg from 'chessground/types';
import { Prop } from 'common';

export type MoveHandler = (fen: Fen, api: CgApi, yourMove?: boolean) => void;

export interface InputOpts {
  input?: HTMLInputElement;
  ctrl: MoveCtrl;
}

export interface SubmitOpts {
  isTrusted: boolean;
  force?: boolean;
  yourMove?: boolean;
}

export type Submit = (v: string, submitOpts: SubmitOpts) => void;

export interface MoveCtrl {
  drop(key: cg.Key, piece: string): void;
  update(step: { fen: string }, yourMove?: boolean): void;
  addHandler(h: MoveHandler | undefined): void;
  isFocused: Prop<boolean>;
  move(orig: cg.Key, dest: cg.Key, roleChar?: string): void;
  select(key: cg.Key): void;
  hasSelected(): cg.Key | undefined;
  confirmMove(): void;
  usedSan: boolean;
  jump(delta: number): void;
  justSelected(): boolean;
  clock(): ClockCtrl | undefined;
  draw(): void;
  next(): void;
  vote(v: boolean): void;
  takeback(): void;
  resign(v: boolean, immediately?: boolean): void;
  rematch(accept?: boolean): boolean;
  modalOpen: Prop<boolean>;
  root: RootCtrl;
  mode: 'round' | 'puzzle';
  redraw: () => void;
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
  keyboard: boolean;
}

export interface VoiceMoveHandler {
  registerMoveCtrl(ctrl: MoveCtrl): void;
  registerModal(modalListener?: Voice.Listener, phrases?: string[]): void;
  available(): [string, string][];
  arrogance(conf?: number): number;
  arrowColors(enabled?: boolean): boolean;
  //arrowNumbers(enabled?: boolean): boolean;
}

export interface KaldiOpts {
  keys: string[];
  audioCtx: AudioContext;
  broadcast: (msgText: string, msgType: Voice.MsgType, words: Voice.WordResult | undefined, forMs: number) => void;
  impl: 'vanilla' | 'worklet';
}
