export type ColorChoice = Color | 'random';

export interface CoordinateTrainerConfig {
  colorPref: ColorChoice;
  i18n: I18nDict;
  is3d: boolean;
  resizePref: number;
  scores: {
    white: number[];
    black: number[];
  };
}

export type CoordModifier = 'next' | 'current';

export type Redraw = () => void;
