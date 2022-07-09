import { Outcome } from 'chessops/types';
import { Prop } from 'common';
import { StoredProp } from 'common/storage';

export type CevalTechnology = 'asmjs' | 'wasm' | 'hce' | 'nnue' | 'external';

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
  setAutoShapes: () => void;
  redraw: () => void;
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
  engineName: string;
  longEngineName(): string | undefined;
  variant: Variant;
  setHovering: (fen: string, uci?: string) => void;
  setPvBoard: (pvBoard: PvBoard | null) => void;
  multiPv: StoredProp<number>;
  start: (path: string, steps: Step[], threatMode?: boolean) => void;
  stop(): void;
  threads(): number;
  setThreads(threads: number): void;
  maxThreads: number;
  hashSize(): number;
  setHashSize(hash: number): void;
  maxHashSize: number;
  infinite: StoredProp<boolean>;
  supportsNnue: boolean;
  enableNnue: StoredProp<boolean>;
  hovering: Prop<Hovering | null>;
  pvBoard: Prop<PvBoard | null>;
  toggle(): void;
  curDepth(): number;
  isDeeper(): boolean;
  destroy(): void;
  redraw(): void;
  disconnectExternalEngine(): void;
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
