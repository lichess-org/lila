import type { CevalCtrl, NodeEvals } from 'ceval';
import type { Prop } from 'common/common';
import type { Deferred } from 'common/defer';
import type { StoredBooleanProp } from 'common/storage';
import type { KeyboardMove } from 'keyboard-move';
import type { EngineCode } from 'shogi/engine-name';
import type { Api as SgApi } from 'shogiground/api';
import type { Config as SgConfig } from 'shogiground/config';
import type { MoveOrDrop, Outcome, Piece } from 'shogiops/types';
import type { Shogi } from 'shogiops/variant/shogi';
import type { TreeWrapper } from 'tree';
import type PuzzleSession from './session';

export interface KeyboardController {
  vm: Vm;
  redraw: Redraw;
  userJump(path: Tree.Path): void;
  getCeval(): CevalCtrl;
  toggleCeval(): void;
  toggleThreatMode(): void;
  playBestMove(): void;
}

export type ThemeKey =
  | 'mix'
  | 'advantage'
  | 'equality'
  | 'crushing'
  | 'opening'
  | 'middlegame'
  | 'endgame'
  | 'oneMove'
  | 'short'
  | 'long'
  | 'veryLong'
  | 'fork'
  | 'pin'
  | 'sacrifice'
  | 'strikingPawn'
  | 'joiningPawn'
  | 'edgeAttack'
  | 'mate'
  | 'mateIn1'
  | 'mateIn3'
  | 'mateIn5'
  | 'mateIn7'
  | 'mateIn9'
  | 'tsume'
  | 'lishogiGames'
  | 'otherSources';
interface AllThemes {
  dynamic: ThemeKey[];
  static: Set<ThemeKey>;
}

export type PuzzleDifficulty = 'easiest' | 'easier' | 'normal' | 'harder' | 'hardest';

export interface Controller extends KeyboardController {
  nextNodeBest(): string | undefined;
  disableThreatMode?: Prop<boolean>;
  outcome(): Outcome | undefined;
  isImpasse(): boolean;
  mandatoryCeval?: Prop<boolean>;
  showEvalGauge: Prop<boolean>;
  currentEvals(): NodeEvals;
  ongoing: boolean;
  playUsi(usi: string): void;
  playUsiList(usiList: string[]): void;
  getOrientation(): Color;
  threatMode: Prop<boolean>;
  getNode(): Tree.Node;
  position(): Shogi;
  showComputer(): boolean;
  getData(): PuzzleData;
  getTree(): TreeWrapper;
  shogiground: SgApi;
  makeSgOpts(): SgConfig;
  viewSolution(): void;
  nextPuzzle(): void;
  vote(v: boolean): void;
  voteTheme(theme: ThemeKey, v: boolean): void;
  pref: PuzzlePrefs;
  difficulty?: PuzzleDifficulty;
  userMove(orig: Key, dest: Key, prom: boolean): void;
  userDrop(piece: Piece, dest: Key, prom: boolean): void;
  autoNext: StoredBooleanProp;
  autoNexting: () => boolean;
  session: PuzzleSession;
  allThemes?: AllThemes;
  keyboardMove?: KeyboardMove;

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
  resultSent: boolean;
  lastFeedback: 'init' | 'fail' | 'win' | 'good';
  initialPath: Tree.Path;
  initialNode: Tree.Node;
  canViewSolution: boolean;
  autoScrollRequested: boolean;
  autoScrollNow: boolean;
  voteDisabled?: boolean;
  showComputer(): boolean;
  showAutoShapes(): boolean;
}

export interface PuzzleOpts {
  pref: PuzzlePrefs;
  data: PuzzleData;
  difficulty?: PuzzleDifficulty;
  themes?: {
    dynamic: string;
    static: string;
  };
}

interface PuzzlePrefs {
  coords: 0 | 1 | 2;
  destination: boolean;
  dropDestination: boolean;
  moveEvent: number;
  highlightLastDests: boolean;
  highlightCheck: boolean;
  squareOverlay: boolean;
  animation: {
    duration: number;
  };
  blindfold: boolean;
  resizeHandle: number;
  keyboardMove: boolean;
}

interface Theme {
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
  player: { color: Color };
}

export interface PuzzleReplay {
  i: number;
  of: number;
  days: number;
}

// todo - separate outside sources and lishogi games
export interface PuzzleGame {
  // From games
  id?: string;
  perf?: {
    icon: string;
    name: string;
  };
  rated?: boolean;
  players?: [PuzzlePlayer, PuzzlePlayer];
  moves?: string;
  clock?: string;
  // From the outside
  sfen?: string;
  author?: string;
  description?: string;
}

export interface PuzzlePlayer {
  userId: string;
  name: string;
  title?: string;
  ai?: number;
  aiCode?: EngineCode;
  color: Color;
}

interface PuzzleUser {
  id: string;
  rating: number;
  provisional?: boolean;
}

export interface Puzzle {
  id: string;
  solution: Usi[];
  ambPromotions: number[];
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

export type RoundThemes = Record<ThemeKey, boolean>;

interface PuzzleRound {
  win: boolean;
  ratingDiff: number;
  themes?: RoundThemes;
}

export interface MoveTest {
  move: MoveOrDrop;
  sfen: Sfen;
  path: Tree.Path;
}
