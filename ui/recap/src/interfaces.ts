interface NbStreak {
  nb: number;
  streak: number;
}
export interface Results {
  win: number;
  draw: number;
  loss: number;
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

export interface Recap {
  year: number;
  createdAt: number;
  puzzles: {
    nb: NbStreak;
    results: Results;
    votes: {
      up: number;
      down: number;
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
    nb: NbStreak;
    opponents: Counted<LightUser>[];
    timePlaying: number;
    sources: {
      friend: number;
      simul: number;
      pool: number;
      ai: number;
      arena: number;
    };
    firstMoves: Counted<string>[];
    results: Results;
  };
}
