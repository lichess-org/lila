import { Clock, Player, Source, Status } from 'game';
import { VNode } from 'snabbdom';
import AnalyseController from './ctrl';
import { ForecastData } from './forecast/interfaces';
import { Goal as PracticeGoal, StudyPracticeData } from './study/practice/interfaces';

export type Seconds = number;

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
  tags?: string[][];
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
  plies: number;
  startedAtPly: number;
  startedAtStep: number;
  source: Source;
  speed: Speed;
  variant: Variant;
  winner?: Color;
  moveCentis?: number[];
  initialSfen?: string;
  importedBy?: string;
  division?: Division;
  perf: string;
  rated?: boolean;
}

export interface Opening {
  english: string;
  japanese: string;
  ply: number;
}

export interface Division {
  middle?: number;
  end?: number;
}

export interface Analysis {
  id: string;
  sente: AnalysisSide;
  gote: AnalysisSide;
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
  initialPly?: number | string;
  userId: string | null;
  hunter: boolean;
  embed: boolean;
  socketSend: SocketSend;
  trans: Trans;
  study?: any;
  tagTypes?: string;
  practice?: StudyPracticeData;
  $side?: JQuery;
  $underboard?: JQuery;
  i18n: any;
  chat: any;
}

export type Conceal = boolean | 'conceal' | 'hide' | null;
export type ConcealOf = (isMainline: boolean) => (path: Tree.Path, node: Tree.Node) => Conceal;

export type Redraw = () => void;
