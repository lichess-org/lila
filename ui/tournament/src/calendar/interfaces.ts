export interface Tournament {
  [key: string]: any;
}
export interface Ctrl {
  data: {
    since: number;
    to: number;
    tournaments: Tournament[];
  };
}

export type Lanes = Array<Array<Tournament>>;
