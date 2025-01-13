import type { Prop } from 'common/common';
import type { Config, PuzPrefs, Puzzle } from 'puz/interfaces';

export interface StormOpts {
  data: StormData;
  pref: StormPrefs;
}

export interface StormPrefs extends PuzPrefs {}

export interface StormData {
  puzzles: Puzzle[];
  notAnExploit: string;
  key?: string;
  signed?: string;
}

export interface StormVm {
  response?: RunResponse;
  dupTab?: boolean;
  signed: Prop<string | undefined>;
  lateStart: boolean;
  filterFailed: boolean;
  filterSlow: boolean;
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

interface NewHigh {
  key: 'day' | 'week' | 'month' | 'allTime';
  prev: number;
}

export interface StormConfig extends Config {
  timeToStart: number;
}
