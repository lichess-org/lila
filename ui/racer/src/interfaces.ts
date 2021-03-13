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
  players: PlayerWithMoves[];
  startsIn?: number;
}

export interface RacerData extends UpdatableData {
  race: Race;
  puzzles: Puzzle[];
  player: Player;
  key?: string;
}

export interface Race {
  id: string;
  moves: number;
}

export interface Player {
  name: string;
  userId?: string;
  title?: string;
  end?: boolean;
}
export interface PlayerWithMoves extends Player {
  moves: number;
}

export interface RacerVm {
  startsAt?: Date;
  signed: Prop<string | undefined>;
}

export interface RacerConfig extends Config {}
