import perfIcons from 'common/perfIcons';

export interface Tournament {
  id: string;
  fullName: string;
  schedule: {
    freq: string;
    speed: string;
  };
  perf: {
    key: keyof typeof perfIcons;
    position: number;
    name: string;
  };
  hasMaxRating: boolean;
  variant: Variant;
  startsAt: number;
  finishesAt: number;
  status: number;
  position: number;
  rated: boolean;
  minutes: number;
  createdBy: string;
  clock: Clock;
  nbPlayers: number;
}

export interface Data {
  created: Tournament[];
  started: Tournament[];
  finished: Tournament[];
}

export interface Opts {
  data: Data;
  i18n: I18nDict;
}

export interface Clock {
  limit: number;
  increment: number;
}

export interface Ctrl {
  data(): Data;
  trans: Trans;
}

export type Lane = Tournament[];
