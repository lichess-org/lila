export interface Tournament {
  [key: string]: any;
}
export interface Ctrl {
  trans: Trans;
  data: {
    since: number;
    to: number;
    tournaments: Tournament[];
  };
}

export type Lanes = Array<Array<Tournament>>;
