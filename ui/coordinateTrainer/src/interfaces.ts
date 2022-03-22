export type ColorChoice = Color | 'random';

export type Mode = 'findSquare' | 'nameSquare';

interface WhiteBlackScores {
  white: number[];
  black: number[];
}

export interface ModeScores {
  findSquare: WhiteBlackScores;
  nameSquare: WhiteBlackScores;
}

export interface CoordinateTrainerConfig {
  colorPref: ColorChoice;
  i18n: I18nDict;
  is3d: boolean;
  modePref: Mode;
  resizePref: number;
  scores: ModeScores;
}

export type CoordModifier = 'next' | 'current';

export type Redraw = () => void;
