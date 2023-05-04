import { Outcome } from 'chessops/types';
import { Prop } from 'common';
import CevalCtrl from './ctrl';
import { ExternalEngine } from './worker';
import { Api as ChessgroundApi } from 'chessground/api';
import { TreeWrapper } from 'tree';

export interface Eval {
  cp?: number;
  mate?: number;
}

export interface Work {
  variant: VariantKey;
  threads: number;
  hashSize: number | undefined;
  stopRequested: boolean;

  path: string;
  maxDepth: number;
  multiPv: number;
  ply: number;
  threatMode: boolean;
  initialFen: string;
  currentFen: string;
  moves: string[];
  emit: (ev: Tree.LocalEval) => void;
}

export interface EvalMeta {
  path: string;
  threatMode: boolean;
}

export type Redraw = () => void;

export interface CevalOpts {
  storageKeyPrefix?: string;
  multiPvDefault?: number;
  possible: boolean;
  variant: Variant;
  initialFen: string | undefined;
  emit: (ev: Tree.LocalEval, meta: EvalMeta) => void;
  setAutoShapes: () => void;
  redraw: Redraw;
  externalEngines?: ExternalEngine[];
  engineChanged(): void;
  showServerAnalysis: boolean;
  getChessground(): ChessgroundApi;
  tree: TreeWrapper;
  getPath(): string;
  getRetro?(): any;
  getPractice?(): any;
  getStudyPractice?(): any;
  togglePractice?(): void;
  evalCache?: any;
  getNode(): Tree.Node;
  outcome(): Outcome | undefined;
  getNodeList(): Array<Tree.Node>;
}

export interface Hovering {
  fen: string;
  uci: string;
}

export interface PvBoard {
  fen: string;
  uci: string;
}

export interface Started {
  path: string;
  steps: Step[];
  threatMode: boolean;
}

export interface ParentCtrl {
  redraw: Redraw;
  getCeval(): CevalCtrl;
  nextNodeBest(): string | undefined;
  outcome(): Outcome | undefined;
  mandatoryCeval: Prop<boolean>;
  showEvalGauge: Prop<boolean>;
  currentEvals(): NodeEvals;
  ongoing: boolean;
  playUci(uci: string): void;
  playUciList(uciList: string[]): void;
  getOrientation(): Color;
  getNode(): Tree.Node;
  trans: Trans;
  showServerAnalysis: boolean;
  disableThreatMode(): boolean;
  hasServerEval?(): boolean;
  canRequestServerEval?(): boolean;
  requestServerEval?(): void;
  isStudy?: boolean;
  setShowServerComments?(show: boolean): void;
}

export interface NodeEvals {
  local?: Tree.ClientEval;
  server?: Tree.ServerEval;
}

export interface Step {
  ply: number;
  fen: string;
  san?: string;
  uci?: string;
  threat?: Tree.ClientEval;
  ceval?: Tree.ClientEval;
}

export type EngineType = 'disabled' | 'local' | 'server' | `external-${string}`;
export type FallbackType = 'disabled' | 'complement' | 'overwrite';
