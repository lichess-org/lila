interface NbStreak {
  nb: number;
  streak: number;
}
interface Results {
  win: number;
  draw: number;
  loss: number;
}
interface Opening {
  ref: {
    eco: string;
    name: string;
  };
  name: string;
  family: {
    key: string;
    name: string;
  };
}
interface Counted<A> {
  value: A;
  count: number;
}
interface ByColor<A> {
  white: A;
  black: A;
}

export interface Recap {
  year: number;
  id: string;
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
    opponent: {
      value: string;
      count: number;
    };
    timePlaying: number;
    sources: {
      friend: number;
      simul: number;
      pool: number;
      ai: number;
      arena: number;
    };
    firstMove: {
      value: string;
      count: number;
    };
    results: Results;
  };
}
