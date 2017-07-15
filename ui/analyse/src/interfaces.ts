import { Player, Status, Source } from 'game';
import * as cg from 'chessground/types';
import { Goal as PracticeGoal } from './study/practice/interfaces';

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
  forecast?: any;
  treeParts: Tree.Node[];
  evalPut?: boolean;
  practiceGoal?: PracticeGoal;
  pref: any;
}
export interface AnalyseDataWithTree extends AnalyseData {
  tree: Tree.Node;
}

// similar, but not identical, to game/Game
export interface Game {
  id: string;
  status: Status;
  player: Color;
  turns: number;
  startedAtTurn: number;
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
  white: AnalysisSide;
  black: AnalysisSide;
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
  practice?: any;
  onChange?: (fen: Fen, path: Tree.Path, mainlinePly: Ply | false) => void;
  onToggleComputer?: (v: boolean) => void;
}

export interface CgDests {
  [key: string]: cg.Key[]
}

export interface JustCaptured extends cg.Piece {
  promoted?: boolean;
}

export type Conceal = boolean | 'conceal' | 'hide' | null;
export type ConcealOf = (isMainline: boolean) => (path: Tree.Path, node: Tree.Node) => Conceal;
