import { Player, Tournament, Simul, Clock, Status, Source } from 'game';
import * as cg from 'chessground/types';

export type MaybeVNode = VNode | null | undefined;
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
  tournament?: Tournament;
  simul?: Simul;
  takebackable: boolean;
  clock?: Clock;
  analysis?: Analysis;
  userAnalysis: boolean;
  forecast?: any;
  treeParts: Tree.Node[];
  evalPut?: boolean;
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
  userId: string;
  embed: boolean;
  explorer: boolean;
  socketSend: any;
  i18n: any;
  study?: any;
  tagTypes?: string;
  practice?: any;
  onChange?: (fen: Fen, path: Tree.Path, mainlinePly: Ply | false) => void;
  onToggleComputer(v: boolean): void;
}

export interface Study {
  setChapter(id: string): void;
  currentChapter(): StudyChapter;
  data: StudyData;
  socketHandlers: { [key: string]: any };
  vm: any;
}

export interface StudyData {
  id: string;
}

export interface StudyChapter {
  id: string;
}

export interface StudyPractice {
}

export interface CgDests {
  [key: string]: cg.Key[]
}

export interface JustCaptured extends cg.Piece {
  promoted?: boolean;
}
