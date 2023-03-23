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
  voice: VoiceCtrl; // convenience
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

export type MsgType = 'phrase' | 'partial' | 'status' | 'error' | 'stop' | 'start';

export type VoiceListener = (msgText: string, msgType: MsgType, words?: WordResult) => void;

export interface VoiceCtrl {
  // keep rounds, puzzle, move stuff out of this
  setVocabulary: (vocabulary: string[]) => Promise<void>;
  start: () => Promise<void>; // initialize if necessary and begin recording
  stop: () => void; // stop recording/downloading/whatever
  readonly isBusy: boolean; // are we downloading, extracting, or loading?
  readonly isRecording: boolean; // are we recording?
  readonly status: string; // errors, progress, or the most recent voice command
  addListener: (name: string, listener: VoiceListener) => void;
}

export interface VoiceMoveHandler {
  registerMoveCtrl(ctrl: MoveCtrl): void;
  registerModal(modalListener?: VoiceListener, phrases?: string[]): void;
  available(): [string, string][];
  arrogance(conf?: number): number;
  arrowColors(enabled?: boolean): boolean;
  //arrowNumbers(enabled?: boolean): boolean;
}

export interface KaldiOpts {
  keys: string[];
  audioCtx: AudioContext;
  broadcast: (msgText: string, msgType: MsgType, words: WordResult | undefined, forMs: number) => void;
  impl: 'vanilla' | 'worklet';
}

export type WordResult = Array<{
  conf: number;
  start: number;
  end: number;
  word: string;
}>;
