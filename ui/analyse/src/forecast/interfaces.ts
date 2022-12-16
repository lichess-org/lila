import { Prop } from 'common/common';

export interface ForecastData {
  onMyTurn?: boolean;
  steps?: ForecastStep[][];
}

export interface ForecastStep {
  ply: Ply;
  usi: Usi;
  notation: string;
  sfen: Sfen;
  check?: boolean;
}

export interface ForecastCtrl {
  addNodes(fc);
  reloadToLastPly();
  truncate(fc: ForecastStep[]): ForecastStep[];
  playAndSave(node: ForecastStep);
  findStartingWithNode(node: ForecastStep): ForecastStep[][];
  isCandidate(fc: ForecastStep[]): boolean;
  addNodes(fc: ForecastStep[]);
  removeIndex(index: number);
  list(): ForecastStep[][];
  loading: Prop<boolean>;
  onMyTurn: boolean;
}
