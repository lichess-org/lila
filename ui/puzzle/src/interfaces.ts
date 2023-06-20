import PuzzleSession from './session';
import { Api as CgApi } from 'chessground/api';
import { CevalCtrl, NodeEvals } from 'ceval';
import { Config as CgConfig } from 'chessground/config';
import { Deferred } from 'common/defer';
import { Outcome, Move } from 'chessops/types';
import { Prop, Toggle } from 'common';
import { StoredProp } from 'common/storage';
import { TreeWrapper } from 'tree';
import { VNode } from 'snabbdom';
import PuzzleStreak from './streak';
import { PromotionCtrl } from 'chess/promotion';
import { KeyboardMove } from 'keyboardMove';
import { VoiceMove } from 'voice';
import * as Prefs from 'common/prefs';
import perfIcons from 'common/perfIcons';
import { Redraw } from 'common/snabbdom';

export type PuzzleId = string;

export interface KeyboardController {
  vm: Vm;
  redraw: Redraw;
  userJump(path: Tree.Path): void;
  getCeval(): CevalCtrl;
  toggleCeval(): void;
  toggleThreatMode(): void;
  playBestMove(): void;
  flip(): void;
  flipped(): boolean;
  nextPuzzle(): void;
  keyboardHelp: Prop<boolean>;
}

export type ThemeKey = string;
export interface AllThemes {
  dynamic: ThemeKey[];
  static: Set<ThemeKey>;
}

export interface Controller extends KeyboardController {
  nextNodeBest(): string | undefined;
  disableThreatMode?: Prop<boolean>;
  outcome(): Outcome | undefined;
  mandatoryCeval?: Prop<boolean>;
  showEvalGauge: Prop<boolean>;
  currentEvals(): NodeEvals;
  ongoing: boolean;
  playUci(uci: string): void;
  playUciList(uciList: string[]): void;
  getOrientation(): Color;
  threatMode: Prop<boolean>;
  getNode(): Tree.Node;
  showComputer(): boolean;
  trans: Trans;
  getData(): PuzzleData;
  getTree(): TreeWrapper;
  ground: Prop<CgApi | undefined>;
  setChessground(cg: CgApi): void;
  makeCgOpts(): CgConfig;
  viewSolution(): void;
  nextPuzzle(): void;
  vote(v: boolean): void;
  voteTheme(theme: ThemeKey, v: boolean): void;
  pref: PuzzlePrefs;
  settings: PuzzleSettings;
  userMove(orig: Key, dest: Key): void;
  promotion: PromotionCtrl;
  autoNext: StoredProp<boolean>;
  autoNexting: () => boolean;
  rated: StoredProp<boolean>;
  toggleRated: () => void;
  session: PuzzleSession;
  allThemes?: AllThemes;
  showRatings: boolean;
  keyboardMove?: KeyboardMove;
  voiceMove?: VoiceMove;

  streak?: PuzzleStreak;
  skip(): void;

  path?: Tree.Path;
  autoScrollRequested?: boolean;

  nvui?: NvuiPlugin;
  menu: Toggle;
}

export interface NvuiPlugin {
  render(ctrl: Controller): VNode;
}

export type ReplayEnd = PuzzleReplay;

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
  userId: string;
  name: string;
  title?: string;
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
