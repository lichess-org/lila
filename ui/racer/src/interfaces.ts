import { Config, PuzPrefs, Puzzle } from 'puz/interfaces';
import { Api as CgApi } from 'chessground/api';

export type RaceStatus = 'pre' | 'racing' | 'post';

export type WithGround = <A>(f: (g: CgApi) => A) => A | false;

export interface RacerOpts {
  data: RacerData;
  pref: RacerPrefs;
  i18n: I18nDict;
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
}

export interface Race {
  id: string;
  lobby?: boolean;
}

export interface Player {
  name: string;
  userId?: string;
  title?: string;
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
