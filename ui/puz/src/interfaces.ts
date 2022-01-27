import type { Clock } from './clock';
import type { Combo } from './combo';
import type CurrentPuzzle from './current';

export interface PuzPrefs {
  coords: Prefs.Coords;
  is3d: boolean;
  destination: boolean;
  rookCastle: boolean;
  moveEvent: number;
  highlight: boolean;
  animation: number;
}

export type UserMove = (orig: Key, dest: Key) => void;

export interface Puzzle {
  id: string;
  fen: string;
  line: string;
  rating: number;
}

export interface Run {
  pov: Color;
  moves: number;
  errors: number;
  current: CurrentPuzzle;
  clock: Clock;
  history: Round[];
  combo: Combo;
  modifier: Modifier;
  endAt?: number;
}

export interface Round {
  puzzle: Puzzle;
  win: boolean;
  millis: number;
}

export interface Modifier {
  moveAt: number;
  malus?: TimeMod;
  bonus?: TimeMod;
}

export interface TimeMod {
  seconds: number;
  at: number;
}

export interface Config {
  clock: {
    initial: number;
    malus: number;
  };
  combo: {
    levels: [number, number][];
  };
}
