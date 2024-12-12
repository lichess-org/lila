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
export interface ByColor<A> {
  white: A;
  black: A;
}
export interface Sources {
  friend: number;
  simul: number;
  swiss: number;
  pool: number;
  ai: number;
  arena: number;
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
    perfs: {
      key: string;
      seconds: number;
      games: number;
    }[];
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
