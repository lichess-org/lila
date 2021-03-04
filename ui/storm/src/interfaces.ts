import { Prop } from 'common';
import { PuzPrefs, Puzzle } from 'puz/interfaces';

export interface StormOpts {
  data: StormData;
  pref: StormPrefs;
  i18n: any;
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
