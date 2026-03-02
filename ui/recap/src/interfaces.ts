export interface Opts {
  recap?: Recap;
  user: LightUser;
  navigation: boolean;
  costs?: {
    amount: number;
    currency: string;
  };
}

interface NbWin {
  total: number;
  win: number;
}
export interface Opening {
  key: string;
  name: string;
  pgn: string;
}
export interface Counted<A> {
  value: A;
  count: number;
}
export interface Sources {
  friend: number;
  simul: number;
  swiss: number;
  pool: number;
  lobby: number;
  ai: number;
  arena: number;
}

export interface RecapPerf {
  key: Exclude<Perf, 'fromPosition'>;
  games: number;
}

export interface Recap {
  year: number;
  createdAt: number;
  puzzles: {
    nbs: NbWin;
    votes: {
      nb: number;
      themes: number;
    };
  };
  games: {
    perfs: RecapPerf[];
    moves: number;
    openings: ByColor<Counted<Opening>>;
    nbs: NbWin;
    nbWhite: number;
    opponents: Counted<LightUser>[];
    timePlaying: number;
    sources: Sources;
    firstMoves: Counted<string>[];
  };
}
