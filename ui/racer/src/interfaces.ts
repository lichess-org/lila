import { Prop } from 'common';
import { Config, PuzPrefs, Puzzle } from 'puz/interfaces';
import { Api as CgApi } from 'chessground/api';

export type RaceStatus = 'pre' | 'racing' | 'post';

export type WithGround = <A>(f: (g: CgApi) => A) => A | false;

export interface RacerOpts {
  data: RacerData;
  pref: RacerPrefs;
  i18n: any;
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
}

export interface Player {
  name: string;
  userId?: string;
  title?: string;
  end?: boolean;
}
export interface PlayerWithScore extends Player {
  score: number;
  boostAt?: Date;
}

export interface RacerVm {
  startsAt?: Date;
  alreadyStarted: boolean;
}

export interface RacerConfig extends Config {}
