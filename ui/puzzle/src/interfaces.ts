import { Api as CgApi } from 'chessground/api';
import { CevalCtrl, NodeEvals } from 'ceval';
import { Config as CgConfig } from 'chessground/config';
import { Outcome } from 'chessops/types';
import { Prop } from 'common';
import { Role, Move } from 'chessops/types';
import { TreeWrapper } from 'tree';
import { VNode } from 'snabbdom/vnode';
import { StoredBooleanProp } from 'common/storage';
import PuzzleSession from './session';

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
  showComputer(): boolean;
  trans: Trans;
  getData(): PuzzleData;
  getTree(): TreeWrapper;
  ground: Prop<CgApi | undefined>;
  makeCgOpts(): CgConfig;
  viewSolution(): void;
  nextPuzzle(): void;
  vote(v: boolean): void;
  pref: PuzzlePrefs;
  userMove(orig: Key, dest: Key): void;
  promotion: any;
  autoNext: StoredBooleanProp;
  session: PuzzleSession;

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
  next?: PuzzleData;
  justPlayed?: Key;
  resultSent: boolean;
  lastFeedback: 'init' | 'fail' | 'win' | 'good' | 'retry';
  initialPath: Tree.Path;
  initialNode: Tree.Node;
  canViewSolution: boolean;
  autoScrollRequested: boolean;
  autoScrollNow: boolean;
  cgConfig: CgConfig;
  showComputer(): boolean;
  showAutoShapes(): boolean;
}

export interface PuzzleOpts {
  pref: PuzzlePrefs;
  data: PuzzleData;
  i18n: { [key: string]: string | undefined };
}

export interface PuzzlePrefs {
  coords: 0 | 1 | 2;
  is3d: boolean;
  destination: boolean;
  rookCastle: boolean;
  moveEvent: number;
  highlight: boolean;
  resizeHandle: number;
  animation: {
    duration: number;
  };
  blindfold: boolean;
}

export interface Theme {
  key: ThemeKey;
  name: string;
  desc: string;
}

export interface PuzzleData {
  puzzle: Puzzle;
  theme: Theme;
  game: PuzzleGame;
  user: PuzzleUser | undefined;
}

export interface PuzzleGame {
  id: string;
  perf: {
    icon: string;
    name: string;
  };
  rated: boolean;
  players: Array<{ userId: string, name: string, color: Color }>;
  pgn: San[];
  clock: string;
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
}

export interface PuzzleRound {
  win: boolean;
  ratingDiff: number;
  vote?: boolean;
}

export interface Promotion {
  start(orig: Key, dest: Key, callback: (orig: Key, dest: Key, prom: Role) => void): boolean;
  cancel(): void;
  view(): MaybeVNode;
}

export interface MoveTest {
  move: Move,
  fen: Fen;
  path: Tree.Path;
}
