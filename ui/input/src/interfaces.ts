import { Api as CgApi } from 'chessground/api';
import * as cg from 'chessground/types';
import { Prop } from 'common';

export type MoveHandler = (fen: Fen, dests?: cg.Dests, yourMove?: boolean) => void;

export interface InputOpts {
  input: HTMLInputElement;
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
  promote(orig: cg.Key, dest: cg.Key, piece: string): void;
  update(step: { fen: string }, yourMove?: boolean): void;
  registerHandler(h: MoveHandler): void;
  isFocused: Prop<boolean>;
  san(orig: cg.Key, dest: cg.Key): void;
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
  resign(v: boolean, immediately?: boolean): void;
  helpModalOpen: Prop<boolean>;
  voiceCtrl: VoiceCtrl; // convenience
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
  resign?: (v: boolean, immediately?: boolean) => void;
  sendMove: (orig: cg.Key, dest: cg.Key, prom: cg.Role | undefined, meta?: cg.MoveMetadata) => void;
  sendNewPiece?: (role: cg.Role, key: cg.Key, isPredrop: boolean) => void;
  submitMove?: (v: boolean) => void;
  userJumpPlyDelta?: (plyDelta: Ply) => void;
  redraw: () => void;
  next?: () => void;
  vote?: (v: boolean) => void;
}

export type VoiceListener = (command: string) => void;

export interface VoiceCtrl {
  load: () => Promise<boolean>; // returns ready() after promise resolves
  start: () => Promise<boolean>; // begin recording if ready otherwise calls load()
  stop: () => void; // stop recording
  ready: () => boolean; // are we all set up to record?
  recording: () => boolean; // are we recording?
  status: () => string; // errors, progress, or the most recent voice command
  addListener: (listener: VoiceListener) => void;
  removeListener: (listener: VoiceListener) => void;
}

export interface VoskOpts {
  speechLookup: Map<string, string>;
  impl: 'vanilla' | 'worklet';
  url: string;
  ctrl: VoiceCtrl;
}
