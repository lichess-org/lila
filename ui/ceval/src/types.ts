import { Prop } from 'common';
import { StoredProp, StoredBooleanProp } from 'common/storage';

export interface Eval {
  cp?: number;
  mate?: number;
}

export interface WorkerOpts {
  variant: VariantKey;
  threads: false | (() => number | string);
  hashSize: false | (() => number | string);
  minDepth: number;
}

export interface Work {
  path: string;
  maxDepth: number;
  multiPv: number;
  ply: number;
  threatMode: boolean;
  initialFen: string;
  currentFen: string;
  moves: string[];
  emit: (ev: Tree.ClientEval) => void;
}

export interface PoolOpts {
  pnacl: string | false;
  wasm: string | false;
  wasmx: string | false;
  asmjs: string;
}

export interface CevalOpts {
  storageKeyPrefix?: string;
  multiPvDefault?: number;
  possible: boolean;
  variant: Variant;
  emit: (ev: Tree.ClientEval, work: Work) => void;
  setAutoShapes: () => void;
  redraw(): void;
}

export interface Hovering {
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
  pnaclSupported: boolean;
  wasmSupported: boolean;
  wasmxSupported: boolean;
  allowed: Prop<boolean>;
  enabled: Prop<boolean>;
  possible: boolean;
  isComputing(): boolean;
  engineName(): string | undefined;
  variant: Variant;
  setHovering: (fen: string, uci?: string) => void;
  multiPv: StoredProp<number>;
  start: (path: string, steps: Step[], threatMode: boolean, deeper: boolean) => void;
  stop(): void;
  threads: StoredProp<number>;
  hashSize: StoredProp<number>;
  infinite: StoredBooleanProp;
  hovering: Prop<Hovering | null>;
  toggle(): void;
  curDepth(): number;
  isDeeper(): boolean;
  destroy(): void;
  redraw(): void;
}

export interface ParentCtrl {
  getCeval(): CevalCtrl;
  nextNodeBest(): string | undefined;
  disableThreatMode?: Prop<Boolean>;
  toggleThreatMode(): void;
  toggleCeval(): void;
  gameOver: (node?: Tree.Node) => 'draw' | 'checkmate' | false;
  mandatoryCeval?: Prop<boolean>;
  showEvalGauge: Prop<boolean>;
  currentEvals(): NodeEvals;
  ongoing: boolean;
  playUci(uci: string): void;
  getOrientation(): Color;
  threatMode(): boolean;
  getNode(): Tree.Node;
  showComputer(): boolean;
  trans: Trans;
}

export interface NodeEvals {
  client?: Tree.ClientEval
  server?: Tree.ServerEval
}

export interface Step {
  ply: number;
  fen: string;
  san?: string;
  uci?: string;
  threat?: Tree.ClientEval;
  ceval?: Tree.ClientEval;
}

export interface Watchdog {
  arm(): void;
  disarm(): void;
  disarmSoon(): void;
  fail(): void;
  good(): boolean;
}
