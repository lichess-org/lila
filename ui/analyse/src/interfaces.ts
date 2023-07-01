import { VNode } from 'snabbdom';
import { Player, Status, Source, Clock } from 'game';
import * as cg from 'chessground/types';
import { ForecastData } from './forecast/interfaces';
import { StudyPracticeData, Goal as PracticeGoal } from './study/practice/interfaces';
import { RelayData } from './study/relay/interfaces';
import { ChatCtrl } from 'chat';
import { ExplorerOpts } from './explorer/interfaces';
import { StudyData } from './study/interfaces';
import { AnalyseSocketSend } from './socket';
import { ExternalEngine } from 'ceval';
import * as Prefs from 'common/prefs';

export type Seconds = number;

export type { Key, Piece } from 'chessground/types';

export interface NvuiPlugin {
  render(): VNode;
}

export interface AnalyseApi {
  socketReceive(type: string, data: any): boolean;
  path(): Tree.Path;
  setChapter(id: string): void;
}

export interface OpeningPuzzle {
  key: string;
  name: string;
  count: number;
}

// similar, but not identical, to game/GameData
export interface AnalyseData {
  game: Game;
  player: Player;
  opponent: Player;
  orientation: Color;
  spectator?: boolean; // for compat with GameData, for game functions
  takebackable: boolean;
  moretimeable: boolean;
  analysis?: Analysis;
  userAnalysis: boolean;
  forecast?: ForecastData;
  sidelines?: Tree.Node[][];
  treeParts: Tree.Node[];
  practiceGoal?: PracticeGoal;
  clock?: Clock;
  pref: AnalysePref;
  userTv?: {
    id: string;
  };
  puzzle?: OpeningPuzzle;
  externalEngines?: ExternalEngine[];
}

export interface AnalysePref {
  coords: Prefs.Coords;
  is3d?: boolean;
  showDests?: boolean;
  rookCastle?: boolean;
  destination?: boolean;
  highlight?: boolean;
  showCaptured?: boolean;
  animationDuration?: number;
  moveEvent: Prefs.MoveEvent;
}

export interface ServerEvalData {
  ch: string;
  analysis?: Analysis;
  tree: Tree.Node;
  division?: Division;
}

export interface CachedEval {
  fen: Fen;
  knodes: number;
  depth: number;
  pvs: Tree.PvDataServer[];
  path: string;
}

// similar, but not identical, to game/Game
export interface Game {
  id: string;
  status: Status;
  player: Color;
  turns: number;
  fen: Fen;
  startedAtTurn?: number;
  source: Source;
  speed: Speed;
  variant: Variant;
  winner?: Color;
  moveCentis?: number[];
  initialFen?: string;
  importedBy?: string;
  division?: Division;
  opening?: Opening;
  perf: string;
  rated?: boolean;
}

export interface Opening {
  name: string;
  eco: string;
  ply: number;
}

export interface Division {
  middle?: number;
  end?: number;
}

export interface Analysis {
  id: string;
  white: AnalysisSide;
  black: AnalysisSide;
  partial: boolean;
}

export interface AnalysisSide {
  acpl: number;
  inaccuracy: number;
  mistake: number;
  blunder: number;
  accuracy: number;
}

export interface AnalyseOpts {
  element: HTMLElement;
  data: AnalyseData;
  userId?: string;
  hunter: boolean;
  explorer: ExplorerOpts;
  socketSend: AnalyseSocketSend;
  trans: Trans;
  study?: StudyData;
  tagTypes?: string;
  practice?: StudyPracticeData;
  relay?: RelayData;
  $side?: Cash;
  $underboard?: Cash;
  i18n: I18nDict;
  chat: {
    parseMoves: boolean;
    instance?: Promise<ChatCtrl>;
  };
  wiki?: boolean;
  inlinePgn?: string;
  externalEngineEndpoint: string;
}

export interface JustCaptured extends cg.Piece {
  promoted?: boolean;
}

export interface EvalGetData {
  fen: Fen;
  path: string;
  variant?: VariantKey;
  mpv?: number;
  up?: boolean;
}

export interface EvalPutData extends Tree.ServerEval {
  variant?: VariantKey;
}

export type Conceal = false | 'conceal' | 'hide' | null;
export type ConcealOf = (isMainline: boolean) => (path: Tree.Path, node: Tree.Node) => Conceal;

export interface AnalyseState {
  root: Tree.Node | undefined;
  path: Tree.Path | undefined;
  flipped: boolean;
}

export type Redraw = () => void;
