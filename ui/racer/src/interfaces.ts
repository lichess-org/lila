import { Prop } from 'common';
import { PuzPrefs, Puzzle } from 'puz/interfaces';

export interface RacerOpts {
  data: RacerData;
  pref: RacerPrefs;
  i18n: any;
}

export interface RacerPrefs extends PuzPrefs {}

export interface RacerData {
  race: Race;
  puzzles: Puzzle[];
  players: Player[];
  owner: boolean;
  key?: string;
}

export interface Race {
  id: string;
  isPlayer: boolean;
  isOwner: boolean;
  startRel?: number;
}

export interface Player {
  index: number;
  user?: LightUser;
  score: number;
}

export interface RacerVm {
  signed: Prop<string | undefined>;
}
