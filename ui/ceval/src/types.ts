import { Outcome } from 'chessops/types';
import { Prop } from 'common';
import CevalCtrl from './ctrl';
import { ExternalEngine } from './worker';
import { Redraw } from 'chessground/types';

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

export interface CevalOpts {
  storageKeyPrefix?: string;
  multiPvDefault?: number;
  possible: boolean;
  variant: Variant;
  initialFen: string | undefined;
  emit: (ev: Tree.LocalEval, meta: EvalMeta) => void;
  redraw: Redraw;
  externalEngines?: ExternalEngine[];
  showServerAnalysis: boolean;
  getPath(): string;
  togglePractice?(): void;
  getNode(): Tree.Node;
  outcome(): Outcome | undefined;
  getNodeList(): Array<Tree.Node>;
  computeAutoShapes(): void;
  disallowed?(): boolean;
  liveToggled?(): void;
  disableThreatMode?(): boolean;

  // TODO(zamfofex): add types to these
  getChessground(): any;
  tree: any;
  getRetro?(): any;
  getPractice?(): any;
  getStudyPractice?(): any;
  evalCache?: any;
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
  disableThreatMode?: Prop<boolean>;
  outcome(): Outcome | undefined;
  mandatoryCeval?: Prop<boolean>;
  currentEvals(): NodeEvals;
  ongoing: boolean;
  playUci(uci: string): void;
  playUciList(uciList: string[]): void;
  getOrientation(): Color;
  getNode(): Tree.Node;
  trans: Trans;
  hasServerEval?(): boolean;
  canRequestServerEval?(): boolean;
  requestServerEval?(): void;
  getExternalEngines?(): ExternalEngine[];
}

export interface NodeEvals {
  client?: Tree.ClientEval;
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
