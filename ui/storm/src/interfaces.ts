import { Prop } from 'common';
import { Combo, Modifier, Puzzle, Run } from 'puz/interfaces';

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
  puzzles: Puzzle[];
  notAnExploit: string;
  key?: string;
  signed?: string;
}

export interface StormVm {
  puzzleIndex: number;
  moveIndex: number;
  clockMs: number;
  history: Round[];
  puzzleStartAt?: number;
  combo: Combo;
  modifier: Modifier;
  run: StormRun;
  dupTab?: boolean;
  signed: Prop<string | undefined>;
  lateStart: boolean;
  filterFailed: boolean;
}

export interface StormRun extends Run {
  response?: RunResponse;
}

export interface Round {
  puzzle: Puzzle;
  win: boolean;
  millis: number;
}

export interface DailyBest {
  score: number;
  prev?: number;
  at: number;
}

export interface StormRecap {
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
