import type { Clock, Player, Source, Status } from 'game';
import type { VNode } from 'snabbdom';
import type AnalyseController from './ctrl';
import type { ForecastData } from './forecast/interfaces';
import type { Goal as PracticeGoal, StudyPracticeData } from './study/practice/interfaces';

export interface NvuiPlugin {
  render(ctrl: AnalyseController): VNode;
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
interface Game {
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

interface Division {
  middle?: number;
  end?: number;
}

interface Analysis {
  id: string;
  sente: AnalysisSide;
  gote: AnalysisSide;
  partial?: boolean;
}

interface AnalysisSide {
  acpl: number;
  inaccuracy: number;
  mistake: number;
  blunder: number;
}

export interface AnalyseOpts {
  data: AnalyseData;
  initialPly?: number | string;
  userId: string | null;
  hunter: boolean;
  mode: 'replay' | 'study' | 'analyse' | 'practice';
  embed: boolean;
  socketSend: Socket.Send;
  study?: any;
  tagTypes?: string;
  practice?: StudyPracticeData;
  $side?: JQuery;
  $underboard?: JQuery;
  chat: any;
  socketUrl: string;
  socketVersion: number;
}

export type Conceal = boolean | 'conceal' | 'hide' | null;
export type ConcealOf = (isMainline: boolean) => (path: Tree.Path, node: Tree.Node) => Conceal;
