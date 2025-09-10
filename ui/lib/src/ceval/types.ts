import type { Outcome } from 'chessops/types';
import type { Prop } from '../common';
import type { Feature } from '../device';
import type CevalCtrl from './ctrl';

export type WinningChances = number;
export type SearchBy = { movetime: number } | { depth: number } | { nodes: number };
export type Search = { by: SearchBy; multiPv: number; indeterminate?: boolean };

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
  emit: (ev: Tree.LocalEval) => void;
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

export interface CevalOpts {
  variant: Variant;
  initialFen: string | undefined;
  emit: (ev: Tree.LocalEval, meta: EvalMeta) => void;
  onUciHover: (hovering: Hovering | null) => void;
  redraw: Redraw;
  search?: Search;
  onSelectEngine?: () => void;
  externalEngines?: ExternalEngineInfoFromServer[];
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
  outcome(): Outcome | undefined;
  showEvalGauge: Prop<boolean>;
  ongoing: boolean;
  playUci(uci: string): void;
  playUciList(uciList: string[]): void;
  getOrientation(): Color;
  threatMode(): boolean;
  getNode(): Tree.Node;
  showAnalysis(): boolean;
  clearCeval: () => void;
  startCeval: () => void;
  cevalEnabled: (enable?: boolean) => boolean | 'force';
  showCeval?: (show?: boolean) => boolean;
  isCevalAllowed?: () => boolean;
  externalEngines?: () => ExternalEngineInfo[] | undefined;
  showFishnetAnalysis?: () => boolean;
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
