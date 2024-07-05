import { Move } from 'chessops/types';
import { VNode } from 'snabbdom';
import * as Prefs from 'common/prefs';
import perfIcons from 'common/perfIcons';
import PuzzleCtrl from './ctrl';
import { FEN } from 'chessground/types';
import { ExternalEngineInfo } from 'ceval';

export type PuzzleId = string;

export type ThemeKey = string;
export interface AllThemes {
  dynamic: ThemeKey[];
  static: Set<ThemeKey>;
}

export interface NvuiPlugin {
  render(ctrl: PuzzleCtrl): VNode;
}

export type ReplayEnd = PuzzleReplay;

export type PuzzleDifficulty = 'easiest' | 'easier' | 'normal' | 'harder' | 'hardest';

export interface PuzzleSettings {
  difficulty: PuzzleDifficulty;
  color?: Color;
}

export interface PuzzleOpts {
  pref: PuzzlePrefs;
  data: PuzzleData;
  i18n: I18nDict;
  settings: PuzzleSettings;
  themes?: {
    dynamic: string;
    static: string;
  };
  showRatings: boolean;
  externalEngineEndpoint: string;
}

export interface PuzzlePrefs {
  coords: Prefs.Coords;
  is3d: boolean;
  destination: boolean;
  rookCastle: boolean;
  moveEvent: number;
  highlight: boolean;
  animation: {
    duration: number;
  };
  blindfold: boolean;
  keyboardMove: boolean;
  voiceMove: boolean;
}

export interface Angle {
  key: ThemeKey;
  name: string;
  desc: string;
  chapter?: string;
  opening?: {
    key: string;
    name: string;
  };
  openingAbstract?: boolean;
}

export interface PuzzleData {
  puzzle: Puzzle;
  angle: Angle;
  game: PuzzleGame;
  user: PuzzleUser | undefined;
  replay?: PuzzleReplay;
  streak?: string;
  externalEngines?: ExternalEngineInfo[];
}

export interface PuzzleReplay {
  i: number;
  of: number;
  days: number;
}

export interface PuzzleGame {
  id: string;
  perf: {
    key: keyof typeof perfIcons;
    name: string;
  };
  rated: boolean;
  players: [PuzzlePlayer, PuzzlePlayer];
  pgn: string;
  clock: string;
}

export interface PuzzlePlayer {
  name: string;
  rating?: number;
  title?: string;
  flair?: string;
  color: Color;
}

export interface PuzzleUser {
  id: string;
  rating: number;
  provisional?: boolean;
}

export interface Puzzle {
  id: PuzzleId;
  solution: Uci[];
  rating: number;
  plays: number;
  initialPly: number;
  themes: ThemeKey[];
}

export interface PuzzleResult {
  round?: PuzzleRound;
  next?: PuzzleData;
  replayComplete?: boolean;
}

export interface RoundThemes {
  [key: string]: boolean;
}

export interface PuzzleRound {
  win: boolean;
  ratingDiff: number;
  themes?: RoundThemes;
}

export interface MoveTest {
  move: Move;
  fen: FEN;
  path: Tree.Path;
}
