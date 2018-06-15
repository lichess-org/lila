import { Player, Status, Source } from 'game';
import * as cg from 'chessground/types';
import { ForecastData } from './forecast/interfaces';
import { StudyPracticeData, Goal as PracticeGoal } from './study/practice/interfaces';
import { RelayData } from './study/relay/interfaces';

export type MaybeVNode = VNode | string | null | undefined;
export type MaybeVNodes = MaybeVNode[]

export { Key, Piece } from 'chessground/types';
import { VNode } from 'snabbdom/vnode'

// similar, but not identical, to game/GameData
export interface AnalyseData {
  game: Game;
  player: Player;
  opponent: Player;
  orientation: Color;
  spectator?: boolean; // for compat with GameData, for game functions
  takebackable: boolean;
  analysis?: Analysis;
  userAnalysis: boolean;
  forecast?: ForecastData;
  treeParts: Tree.Node[];
  evalPut?: boolean;
  practiceGoal?: PracticeGoal;
  pref: any;
}

export interface ServerEvalData {
  ch: string;
  analysis?: Analysis;
  tree: Tree.Node;
  division?: Division;
}

// similar, but not identical, to game/Game
export interface Game {
  id: string;
  status: Status;
  player: Color;
  turns: number;
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
}

export interface Opening {
  name: string;
  eco: string;
  ply: number;
}

export interface Division {
  middle?: number;
  end?: number
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
}

export interface AnalyseOpts {
  element: HTMLElement;
  sideElement: HTMLElement;
  data: AnalyseData;
  initialPly?: number | string;
  userId: string | null;
  embed: boolean;
  explorer: boolean;
  socketSend: SocketSend;
  trans: Trans;
  study?: any;
  tagTypes?: string;
  practice?: StudyPracticeData;
  onToggleComputer?: (v: boolean) => void;
  relay?: RelayData;
}

export interface CgDests {
  [key: string]: cg.Key[]
}

export interface JustCaptured extends cg.Piece {
  promoted?: boolean;
}

export type Conceal = boolean | 'conceal' | 'hide' | null;
export type ConcealOf = (isMainline: boolean) => (path: Tree.Path, node: Tree.Node) => Conceal;

export type Redraw = () => void;
