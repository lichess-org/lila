export interface Tournament {
  id: string;
  bounds: {
    start: Date;
    end: Date;
  };
  startsAt: number;
  minutes: number;
  rated: boolean;
  hasMaxRating: boolean;
  schedule: {
    freq: string;
  };
  fullName: string;
  perf: {
    key: Exclude<Perf, 'fromPosition'>;
  };
}

export interface Ctrl {
  trans: Trans;
  data: Data;
}

export interface Opts {
  data: Data;
  i18n: I18nDict;
}

export interface Data {
  since: number;
  to: number;
  tournaments: Tournament[];
}

export type Lanes = Array<Array<Tournament>>;
