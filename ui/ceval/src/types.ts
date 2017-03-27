export type Color = 'white' | 'black';

export interface VariantInfo {
  key: Variant;
}

export type Variant = 'standard' | 'fromPosition' | 'crazyhouse' | 'chess960' |
                      'kingOfTheHill' | 'threeCheck' | 'antichess' | 'atomic' |
                      'horde' | 'racingKings';

export interface Eval {
  cp?: number;
  mate?: number;
}

export interface PvData {
  moves: string[];
  mate?: number;
  cp?: number;
}

export interface ClientEval {
  fen: string;
  maxDepth: number;
  depth: number;
  knps: number;
  nodes: number;
  millis: number;
  pvs: PvData[];
  cloud?: boolean;
  cp?: number;
  mate?: number;
}

export interface ServerEval {
  cp?: number;
  mate?: number;
}

export interface WorkerOpts {
  variant: Variant;
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
  startedAt?: Date;
  emit: (ev: ClientEval) => void;
}

export interface PoolOpts {
  pnacl: string | false;
  wasm: string | false;
  asmjs: string;
  onCrash: (info: CrashInfo) => void;
}

export interface CrashInfo {
  lastError: any;
  hash: number;
  threads: number;
}

export interface CevalOpts {
  storageKeyPrefix?: string;
  failsafe: boolean;
  multiPvDefault: number;
  possible: boolean;
  variant: VariantInfo;
  onCrash: (info: CrashInfo) => void;
  emit: (ev: ClientEval) => void;
  setAutoShapes: () => void;
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

export interface CevalController {
  goDeeper: () => void;
  canGoDeeper: () => boolean;
  effectiveMaxDepth: () => number;
  pnaclSupported: boolean;
  wasmSupported: boolean;
  allowed: Mithril.Property<boolean>;
  enabled: Mithril.Property<boolean>;
  possible: boolean;
  isComputing: () => boolean;
  variant: VariantInfo;
  setHovering: (fen: string, uci: string | null) => void;
  multiPv: StoredProp<number>;
  start: (path: string, steps: Step[], threatMode: boolean, deeper: boolean) => void;
  stop: () => void;
  threads: StoredProp<number>;
  hashSize: StoredProp<number>;
  infinite: StoredProp<boolean>;
  hovering: Mithril.Property<Hovering | null>;
  toggle: () => void;
  curDepth: () => number;
  isDeeper: () => boolean;
  destroy: () => void;
  env: () => CevalEnv;
}

export interface CevalEnv {
  pnacl: boolean;
  wasm: boolean;
  multiPv: number;
  threads: number;
  hashSize: number;
  maxDepth: number;
}

export interface ParentController {
  getCeval: () => CevalController;
  nextNodeBest: () => boolean;
  disableThreatMode?: Mithril.Property<Boolean>;
  vm: ParentVm;
  toggleThreatMode: () => void;
  toggleCeval: () => void;
  gameOver: () => boolean;
  mandatoryCeval?: Mithril.Property<boolean>;
  showEvalGauge: Mithril.Property<boolean>;
  currentEvals: () => NodeEvals;
  ongoing: boolean;
  playUci: (uci: string) => void;
  getOrientation: () => Color;
}

export interface ParentVm {
  threatMode: boolean;
  node: Node;
  showComputer: () => boolean;
}

export interface Node {
  check: boolean;
  threat?: ClientEval;
  ceval?: ClientEval;
  fen: string;
}

export interface NodeEvals {
  client?: ClientEval
  server?: ServerEval
}

export interface Step {
  ply: number;
  fen: string;
  san?: string;
  uci?: string;
  threat?: ClientEval;
  ceval?: ClientEval;
}
