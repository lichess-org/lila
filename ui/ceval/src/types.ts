import { Outcome } from 'chessops/types';
import { Prop } from 'common';
import { Feature } from 'common/device';
import CevalCtrl from './ctrl';

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

export interface EngineInfo {
  id: string;
  name: string;
  class?: string; // 'NNUE', 'EXTERNAL', 'HCE', etc
  short?: string;
  variants?: VariantKey[];
  maxThreads?: number;
  maxHash?: number;
  defaultDepth?: number;
  requires?: Feature | 'external';
}

export interface ExternalEngineInfo extends EngineInfo {
  clientSecret: string;
  officialStockfish?: boolean;
  endpoint: string;
}

export interface BrowserEngineInfo extends EngineInfo {
  minMem?: number;
  assets: { root?: string; js?: string; wasm?: string; version?: string };
}

export enum CevalState {
  Initial,
  Loading,
  Idle,
  Computing,
  Failed,
}

export interface CevalEngine {
  getState(): CevalState;
  start(work: Work): void;
  stop(): void;
  destroy(): void;
}

export interface EvalMeta {
  path: string;
  threatMode: boolean;
}

export type Redraw = () => void;
export type Progress = (p?: { bytes: number; total: number }) => void;

export interface CevalOpts {
  storageKeyPrefix?: string;
  multiPvDefault?: number;
  possible: boolean;
  variant: Variant;
  initialFen: string | undefined;
  emit: (ev: Tree.LocalEval, meta: EvalMeta) => void;
  setAutoShapes: () => void;
  redraw: Redraw;
  externalEngines?: ExternalEngineInfo[];
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
  getCeval(): CevalCtrl;
  nextNodeBest(): string | undefined;
  disableThreatMode?: Prop<boolean>;
  toggleThreatMode(): void;
  toggleCeval(): void;
  outcome(): Outcome | undefined;
  mandatoryCeval?: Prop<boolean>;
  showEvalGauge: Prop<boolean>;
  currentEvals(): NodeEvals;
  ongoing: boolean;
  playUci(uci: string): void;
  playUciList(uciList: string[]): void;
  getOrientation(): Color;
  threatMode(): boolean;
  getNode(): Tree.Node;
  showComputer(): boolean;
  toggleComputer?: () => void;
  cevalSetMultiPv?: (multiPv: number) => void;
  cevalReset?: () => void;
  redraw?: () => void;
  externalEngines?: () => ExternalEngineInfo[] | undefined;
  trans: Trans;
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
