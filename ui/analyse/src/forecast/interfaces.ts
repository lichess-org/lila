import { Prop } from 'common';

export interface ForecastData {
  onMyTurn?: boolean;
  steps?: ForecastStep[][];
}

export interface ForecastStep {
  ply: Ply;
  uci: Uci;
  san: San;
  fen: Fen;
  check?: Key;
}

export interface ForecastCtrl {
  reloadToLastPly(): void;
  truncate(fc: ForecastStep[]): ForecastStep[];
  playAndSave(node: ForecastStep): void;
  findStartingWithNode(node: ForecastStep): ForecastStep[][];
  isCandidate(fc: ForecastStep[]): boolean;
  addNodes(fc: ForecastStep[]): void;
  removeIndex(index: number): void;
  list(): ForecastStep[][];
  loading: Prop<boolean>;
  onMyTurn: boolean;
}
