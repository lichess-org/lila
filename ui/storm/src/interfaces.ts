import {Role} from 'chessground/types';
import { VNode } from 'snabbdom/vnode'

export type MaybeVNode = VNode | string | null | undefined;
export type MaybeVNodes = MaybeVNode[];
export type Redraw = () => void;

export interface StormOpts {
  data: StormData;
  pref: StormPrefs;
  i18n: any;
}

export interface StormPrefs {
  coords: 0 | 1 | 2;
  is3d: boolean;
  destination: boolean;
  rookCastle: boolean;
  moveEvent: number;
  highlight: boolean;
}

export interface StormData {
  puzzles: StormPuzzle[];
}

export type StormMode = 'play' | 'end';

export interface StormVm {
  mode: StormMode;
  puzzleIndex: number;
  moveIndex: number;
  clock: StormClock;
  history: Round[];
  puzzleStartAt?: number;
  combo: number;
  modifier: StormModifier;
}

export interface Round {
  puzzle: StormPuzzle;
  win: boolean;
  millis: number;
}

export interface StormClock {
  budget: number;
  startAt?: number;
}

export interface StormModifier {
  moveAt: number;
  malus?: TimeMod;
  bonus?: TimeMod;
}

export interface TimeMod {
  seconds: number;
  at: number;
}

export interface StormPuzzle {
  id: string;
  fen: string;
  line: string;
}

export interface Promotion {
  start(orig: Key, dest: Key, callback: (orig: Key, dest: Key, prom: Role) => void): boolean;
  cancel(): void;
  view(): MaybeVNode;
}

