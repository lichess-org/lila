import { Prop } from 'common';
import { PuzPrefs, Puzzle } from 'puz/interfaces';

export interface RacerOpts {
  data: RacerData;
  pref: RacerPrefs;
  i18n: any;
}

export interface RacerPrefs extends PuzPrefs {}

export interface UpdatableData {
  players: Player[];
  startsIn?: number;
  finished?: boolean;
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
  alreadyStarted?: boolean;
}

export interface Player {
  name: string;
  userId?: string;
  title?: string;
  moves: number;
}

export interface RacerVm {
  startsAt?: Date;
  signed: Prop<string | undefined>;
}
