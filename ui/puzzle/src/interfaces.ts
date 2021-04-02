import PuzzleSession from './session';
import { Api as CgApi } from 'shogiground/api';
import { CevalCtrl, NodeEvals } from 'ceval';
import { Config as CgConfig } from 'shogiground/config';
import { Piece } from 'shogiground/types';
import { Deferred } from 'common/defer';
import { Outcome } from 'shogiops/types';
import { Prop } from 'common';
import { Move } from 'shogiops/types';
import { StoredBooleanProp } from 'common/storage';
import { TreeWrapper } from 'tree';
import { VNode } from 'snabbdom/vnode';
import { Shogi } from 'shogiops';

export type MaybeVNode = VNode | string | null | undefined;
export type MaybeVNodes = MaybeVNode[];

export type Redraw = () => void;

export interface KeyboardController {
  vm: Vm;
  redraw: Redraw;
  userJump(path: Tree.Path): void;
  getCeval(): CevalCtrl;
  toggleCeval(): void;
  toggleThreatMode(): void;
  playBestMove(): void;
}

export type ThemeKey = string;
export interface AllThemes {
  dynamic: ThemeKey[];
  static: Set<ThemeKey>;
}

export type PuzzleDifficulty = 'easiest' | 'easier' | 'normal' | 'harder' | 'hardest';

export interface Controller extends KeyboardController {
  nextNodeBest(): string | undefined;
  disableThreatMode?: Prop<boolean>;
  outcome(): Outcome | undefined;
  mandatoryCeval?: Prop<boolean>;
  showEvalGauge: Prop<boolean>;
  currentEvals(): NodeEvals;
  ongoing: boolean;
  playUci(uci: string): void;
  getOrientation(): Color;
  threatMode: Prop<boolean>;
  getNode(): Tree.Node;
  position(): Shogi;
  showComputer(): boolean;
  trans: Trans;
  getData(): PuzzleData;
  data: PuzzleOpts;
  getTree(): TreeWrapper;
  ground: Prop<CgApi | undefined>;
  makeCgOpts(): CgConfig;
  viewSolution(): void;
  nextPuzzle(): void;
  vote(v: boolean): void;
  voteTheme(theme: ThemeKey, v: boolean): void;
  pref: PuzzlePrefs;
  difficulty?: PuzzleDifficulty;
  userMove(orig: Key, dest: Key): void;
  userDrop(piece: Piece, dest: Key): void;
  promotion: any;
  autoNext: StoredBooleanProp;
  autoNexting: () => boolean;
  session: PuzzleSession;
  allThemes?: AllThemes;

  path?: Tree.Path;
  autoScrollRequested?: boolean;
}

export interface Vm {
  path: Tree.Path;
  nodeList: Tree.Node[];
  node: Tree.Node;
  mainline: Tree.Node[];
  pov: Color;
  mode: 'play' | 'view' | 'try';
  round?: PuzzleRound;
  next: Deferred<PuzzleData>;
  justPlayed?: Key;
  justDropped?: Piece;
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
}

export interface PuzzleOpts {
  pref: PuzzlePrefs;
  data: PuzzleData;
  i18n: { [key: string]: string | undefined };
  difficulty?: PuzzleDifficulty;
  themes?: {
    dynamic: string;
    static: string;
  };
}

export interface PuzzlePrefs {
  coords: 0 | 1 | 2;
  is3d: boolean;
  destination: boolean;
  moveEvent: number;
  highlight: boolean;
  animation: {
    duration: number;
  };
  blindfold: boolean;
  pieceNotation: number;
}

export interface Theme {
  key: ThemeKey;
  name: string;
  desc: string;
  chapter?: string;
}

export interface PuzzleData {
  puzzle: Puzzle;
  theme: Theme;
  game: PuzzleGame;
  user: PuzzleUser | undefined;
  replay?: PuzzleReplay;
}

export interface PuzzleReplay {
  i: number;
  of: number;
  days: number;
}

export interface PuzzleGame {
  // From games
  id?: string;
  perf?: {
    icon: string;
    name: string;
  };
  rated?: boolean;
  players?: [PuzzlePlayer, PuzzlePlayer];
  pgn?: string;
  clock?: string;
  // From the outside
  fen?: string;
  author?: string,
  description?: string
}

export interface PuzzlePlayer {
  userId: string;
  name: string;
  title?: string;
  color: Color;
}

export interface PuzzleUser {
  rating: number;
  provisional?: boolean;
}

export interface Puzzle {
  id: string;
  solution: Uci[];
  rating: number;
  plays: number;
  initialPly: number;
  themes: ThemeKey[];
}

export interface PuzzleResult {
  round?: PuzzleRound;
  next: PuzzleData;
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

export interface Promotion {
  start(orig: Key, dest: Key, callback: (orig: Key, dest: Key, prom?: Boolean) => void): boolean;
  cancel(): void;
  view(): MaybeVNode;
}

export interface MoveTest {
  move: Move;
  fen: Fen;
  path: Tree.Path;
}
