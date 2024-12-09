import type { Prop } from 'common';
import type { Config, PuzPrefs, Puzzle } from 'puz/interfaces';

export interface StormOpts {
  puzzles: Puzzle[];
  key?: string;
  pref: StormPrefs;
}

export interface StormPrefs extends PuzPrefs {}

export interface StormData {
  puzzles: Puzzle[];
  key?: string;
  signed?: string;
}

export interface StormVm {
  response?: RunResponse;
  dupTab?: boolean;
  signed: Prop<string | undefined>;
  lateStart: boolean;
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

export interface StormConfig extends Config {
  timeToStart: number;
  minFirstMoveTime: number;
}
