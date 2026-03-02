import type { Prop } from '../index';
import type { Feature } from '../device';
import type CevalCtrl from './ctrl';
import type { VNode } from 'snabbdom';
import type { ClientEval, LocalEval, ServerEval, TreeNode } from '@/tree/types';

export type WinningChances = number;
export type SearchBy = { movetime: number } | { depth: number } | { nodes: number };
export type Search = { by: SearchBy; multiPv: number; indeterminate?: boolean };
export type Millis = number;

export interface Work {
  variant: VariantKey;
  threads: number;
  hashSize: number | undefined;
  gameId: string | undefined; // send ucinewgame when changed
  stopRequested: boolean;

  path: string;
  search: SearchBy;
  multiPv: number;
  ply: number;
  threatMode: boolean;
  initialFen: string;
  currentFen: string;
  moves: string[];
  emit: (ev: LocalEval) => void;
}

export interface BaseEngineInfo {
  id: string;
  name: string;
  short?: string;
  variants?: VariantKey[];
  minThreads?: number;
  maxThreads?: number;
  maxHash?: number;
  requires?: Feature[];
}

export interface ExternalEngineInfoFromServer extends BaseEngineInfo {
  variants: VariantKey[];
  maxHash: number;
  maxThreads: number;
  providerData?: string;
  clientSecret: string;
  officialStockfish?: boolean;
  endpoint: string;
}

export interface ExternalEngineInfo extends ExternalEngineInfoFromServer {
  tech: 'EXTERNAL';
  cloudEval?: false;
}

export interface BrowserEngineInfo extends BaseEngineInfo {
  tech: 'HCE' | 'NNUE';
  short: string;
  minMem?: number;
  assets: { root?: string; js?: string; wasm?: string; version?: string; nnue?: string[] };
  requires: Feature[];
  obsoletedBy?: Feature;
  cloudEval?: boolean;
}

export type EngineInfo = BrowserEngineInfo | ExternalEngineInfo;

export type EngineNotifier = (status?: {
  download?: { bytes: number; total: number };
  error?: string;
}) => void;

export enum CevalState {
  Initial,
  Loading,
  Idle,
  Computing,
  Failed,
}

export interface CevalEngine {
  getInfo(): EngineInfo;
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

export interface CustomCeval {
  search?: () => Search | Millis; // pass number as millis to cap user defined search
  pearlNode?: () => VNode | undefined;
  statusNode?: () => VNode | string | undefined;
}

export interface CevalOpts {
  variant: Variant;
  initialFen: string | undefined;
  emit: (ev: LocalEval, meta: EvalMeta) => void;
  onUciHover: (hovering: Hovering | null) => void;
  redraw: Redraw;
  onSelectEngine?: () => void;
  externalEngines?: ExternalEngineInfoFromServer[];
  custom?: CustomCeval; // hides switch, threat, and go deeper buttons
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
  gameId: string | undefined;
  threatMode: boolean;
}

export interface CevalHandler {
  ceval: CevalCtrl;
  nextNodeBest(): string | undefined;
  toggleThreatMode(v?: boolean): void;
  showEvalGauge: Prop<boolean>;
  ongoing: boolean;
  playUciList(uciList: string[]): void;
  getOrientation(): Color;
  threatMode(): boolean;
  getNode(): TreeNode;
  clearCeval: () => void;
  startCeval: () => void;
  cevalEnabled: (enable?: boolean) => boolean | 'force';
  externalEngines?: () => ExternalEngineInfo[] | undefined;
  showFishnetAnalysis?: () => boolean;
}

export interface NodeEvals {
  client?: ClientEval;
  server?: ServerEval;
}

export interface Step {
  ply: number;
  fen: string;
  san?: string;
  uci?: string;
  threat?: ClientEval;
  ceval?: ClientEval;
}
