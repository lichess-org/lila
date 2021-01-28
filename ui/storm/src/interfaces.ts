import { Role } from 'chessground/types';
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
  notAnExploit: string;
  key?: string;
  signed?: string;
}

export interface StormVm {
  puzzleIndex: number;
  moveIndex: number;
  clock: number;
  history: Round[];
  puzzleStartAt?: number;
  combo: number;
  comboBest: number;
  modifier: StormModifier;
  run: {
    startAt: number;
    endAt?: number;
    moves: number;
    errors: number;
    response?: RunResponse;
  }
  dupTab?: boolean;
}

export interface Round {
  puzzle: StormPuzzle;
  win: boolean;
  millis: number;
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
  rating: number;
}

export interface Promotion {
  start(orig: Key, dest: Key, callback: (orig: Key, dest: Key, prom: Role) => void): boolean;
  cancel(): void;
  view(): MaybeVNode;
}

export interface DailyBest {
  score: number;
  prev?: number;
  at: number;
}

export interface StormRun {
  puzzles: number;
  score: number;
  moves: number;
  errors: number;
  combo: number;
  time: number;
  highest: number;
  signed?: string;
}

export interface RunResponse {
  newHigh?: NewHigh;
}

export interface NewHigh {
  key: 'day' | 'week' | 'month' | 'allTime';
  prev: number;
}
