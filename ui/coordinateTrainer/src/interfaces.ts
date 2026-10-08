export type TimeControl = 'untimed' | 'thirtySeconds';

export type Mode = 'findSquare' | 'nameSquare';

export type InputMethod = 'text' | 'buttons';

type WhiteBlackScores = ByColor<number[]>;

export interface ModeScores {
  findSquare: WhiteBlackScores;
  nameSquare: WhiteBlackScores;
}

export interface CoordinateTrainerConfig {
  is3d: boolean;
  resizePref: number;
  scores: ModeScores;
}

export type CoordModifier = 'next' | 'current';
