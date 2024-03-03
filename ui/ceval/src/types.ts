import { Prop } from 'common/common';
import { StoredBooleanProp, StoredProp } from 'common/storage';
import { Outcome } from 'shogiops/types';

export type CevalTechnology = 'hce' | 'nnue' | 'none'; // at least show cloud analysis for none

export interface Eval {
  cp?: number;
  mate?: number;
}

export interface Config {
  threads: number;
  hashSize: number;
}

export interface Work {
  variant: VariantKey;
  threads: number;
  hashSize: number;
  enteringKingRule: boolean;
  stopRequested: boolean;

  path: string;
  maxDepth: number;
  multiPv: number;
  ply: number;
  threatMode: boolean;
  initialSfen: string;
  currentSfen: string;
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
  initialSfen: string | undefined;
  emit: (ev: Tree.LocalEval, meta: EvalMeta) => void;
  setAutoShapes: () => void;
  redraw: () => void;
}

export interface Hovering {
  sfen: string;
  usi: string;
}

export interface PvBoard {
  sfen: string;
  usi: string;
}

export interface Started {
  path: string;
  steps: Step[];
  threatMode: boolean;
}

export interface CevalCtrl {
  goDeeper(): void;
  canGoDeeper(): boolean;
  effectiveMaxDepth(): number;
  technology: CevalTechnology;
  downloadProgress: Prop<number>;
  allowed: Prop<boolean>;
  enabled: Prop<boolean>;
  possible: boolean;
  analysable: boolean;
  cachable: boolean;
  isComputing(): boolean;
  engineName: string | undefined;
  variant: Variant;
  setHovering: (sfen: string, usi?: string) => void;
  setPvBoard: (pvBoard: PvBoard | null) => void;
  multiPv: StoredProp<number>;
  enteringKingRule: StoredBooleanProp;
  start: (path: string, steps: Step[], threatMode?: boolean) => void;
  stop(): void;
  threads(): number;
  setThreads(threads: number): void;
  maxThreads: number;
  hashSize(): number;
  setHashSize(hash: number): void;
  maxHashSize: number;
  infinite: StoredBooleanProp;
  supportsNnue: boolean;
  shouldUseYaneuraou: boolean;
  enableNnue: StoredBooleanProp;
  hovering: Prop<Hovering | null>;
  pvBoard: Prop<PvBoard | null>;
  toggle(): void;
  curDepth(): number;
  isDeeper(): boolean;
  destroy(): void;
  redraw(): void;
}

export interface ParentCtrl {
  getCeval(): CevalCtrl;
  nextNodeBest(): string | undefined;
  disableThreatMode?: Prop<boolean>;
  toggleThreatMode(): void;
  toggleCeval(): void;
  outcome(): Outcome | undefined;
  isImpasse(): boolean;
  mandatoryCeval?: Prop<boolean>;
  showEvalGauge: Prop<boolean>;
  currentEvals(): NodeEvals;
  ongoing: boolean;
  playUsi(usi: string): void;
  playUsiList(usiList: string[]): void;
  getOrientation(): Color;
  threatMode(): boolean;
  getNode(): Tree.Node;
  showComputer(): boolean;
  trans: Trans;
}

export interface NodeEvals {
  client?: Tree.ClientEval;
  server?: Tree.ServerEval;
}

export interface Step {
  ply: number;
  sfen: string;
  usi?: string;
  notation?: string;
  threat?: Tree.ClientEval;
  ceval?: Tree.ClientEval;
}
