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
  owner: boolean;
  key?: string;
}

export interface Race {
  id: string;
  players: Player[];
  startRel?: number;
}

export interface Player {
  index: number;
  user?: LightUser;
}

export interface RacerVm {
  signed: Prop<string | undefined>;
}
