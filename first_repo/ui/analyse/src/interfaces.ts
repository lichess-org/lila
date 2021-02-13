import { VNode } from 'snabbdom/vnode';
import { Player, Status, Source, Clock } from 'game';
import * as cg from 'chessground/types';
import { ForecastData } from './forecast/interfaces';
import { StudyPracticeData, Goal as PracticeGoal } from './study/practice/interfaces';
import { RelayData } from './study/relay/interfaces';
import AnalyseController from './ctrl';
import { ChatCtrl } from 'chat';

export type MaybeVNode = VNode | string | null | undefined;
export type MaybeVNodes = MaybeVNode[];
export type Seconds = number;

export { Key, Piece } from 'chessground/types';

export interface NvuiPlugin {
  render(ctrl: AnalyseController): VNode;
}

export interface AnalyseApi {
  socketReceive(type: string, data: any): boolean;
  path(): Tree.Path;
  setChapter(id: string): void;
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
  treeParts: Tree.Node[];
  evalPut?: boolean;
  practiceGoal?: PracticeGoal;
  clock?: Clock;
  pref: any;
  url: {
    socket: string;
  };
  userTv?: {
    id: string;
  };
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
}

export interface AnalyseOpts {
  element: HTMLElement;
  data: AnalyseData;
  userId: string | null;
  hunter: boolean;
  embed: boolean;
  explorer: boolean;
  socketSend: SocketSend;
  trans: Trans;
  study?: any;
  tagTypes?: string;
  practice?: StudyPracticeData;
  relay?: RelayData;
  $side?: Cash;
  $underboard?: Cash;
  i18n: any;
  chat: {
    parseMoves: boolean;
    instance?: Promise<ChatCtrl>;
  };
}

export interface JustCaptured extends cg.Piece {
  promoted?: boolean;
}

export type Conceal = boolean | 'conceal' | 'hide' | null;
export type ConcealOf = (isMainline: boolean) => (path: Tree.Path, node: Tree.Node) => Conceal;

export type Redraw = () => void;
