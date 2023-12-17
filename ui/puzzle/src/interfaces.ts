import { Config as CgConfig } from 'chessground/config';
import { Deferred } from 'common/defer';
import { Move } from 'chessops/types';
import { VNode } from 'snabbdom';
import * as Prefs from 'common/prefs';
import perfIcons from 'common/perfIcons';
import PuzzleCtrl from './ctrl';

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

// #TODO wut
export interface Vm {
  path: Tree.Path;
  nodeList: Tree.Node[];
  node: Tree.Node;
  mainline: Tree.Node[];
  pov: Color;
  mode: 'play' | 'view' | 'try';
  round?: PuzzleRound;
  next: Deferred<PuzzleData | ReplayEnd>;
  justPlayed?: Key;
  resultSent: boolean;
  lastFeedback: 'init' | 'fail' | 'win' | 'good' | 'retry';
  initialPath: Tree.Path;
  initialNode: Tree.Node;
  canViewSolution: boolean;
  autoScrollRequested: boolean;
  autoScrollNow: boolean;
  voteDisabled?: boolean;
  cgConfig: CgConfig;
  showComputer(): boolean;
  showAutoShapes(): boolean;
  isDaily: boolean;
}

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
}

export interface PuzzleData {
  puzzle: Puzzle;
  angle: Angle;
  game: PuzzleGame;
  user: PuzzleUser | undefined;
  replay?: PuzzleReplay;
  streak?: string;
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
  fen: Fen;
  path: Tree.Path;
}
