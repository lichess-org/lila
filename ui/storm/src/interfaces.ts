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

export type StormMode = 'play';

export interface StormVm {
  mode: StormMode;
  puzzleIndex: number;
  moveIndex: number;
  clock: StormClock;
}

export interface StormClock {
  budget: number;
  startAt?: number;
  malusAt?: number;
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

