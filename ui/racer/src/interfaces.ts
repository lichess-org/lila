import type { Config, PuzPrefs, Puzzle } from 'lib/puz/interfaces';

export type RaceStatus = 'pre' | 'racing' | 'post';

export type PlayerId = string;

export type Vehicle = number;

export interface RacerOpts {
  data: RacerData;
  pref: RacerPrefs;
}

export interface RacerPrefs extends PuzPrefs {}

export interface UpdatableData {
  players: PlayerWithScore[];
  startsIn?: number;
}

export interface RacerData extends UpdatableData {
  race: Race;
  puzzles: Puzzle[];
  player: Player;
  owner?: boolean;
}

export interface Race {
  id: string;
  lobby?: boolean;
}

export interface Player {
  name: string;
  id?: string;
  title?: string;
  flair?: Flair;
}
export interface PlayerWithScore extends Player {
  score: number;
  boostAt?: Date;
}

export interface RacerVm {
  startsAt?: Date;
  alreadyStarted: boolean;
}

export interface RacerConfig extends Config {
  minFirstMoveTime: number;
}
