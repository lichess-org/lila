export type ColorChoice = Color | 'random';

export type Mode = 'findSquare' | 'nameSquare';

export interface CoordinateTrainerConfig {
  colorPref: ColorChoice;
  i18n: I18nDict;
  is3d: boolean;
  modePref: Mode;
  resizePref: number;
  scores: {
    white: number[];
    black: number[];
  };
}

export type CoordModifier = 'next' | 'current';

export type Redraw = () => void;
