import type { Prop } from 'common/common';

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
  addNodes: (fc: ForecastStep[]) => void;
  reloadToLastPly: () => void;
  truncate: (fc: ForecastStep[]) => ForecastStep[];
  playAndSave: (node: ForecastStep) => void;
  findStartingWithNode: (node: ForecastStep) => ForecastStep[][];
  isCandidate: (fc: ForecastStep[]) => boolean;
  removeIndex: (index: number) => void;
  list(): ForecastStep[][];
  loading: Prop<boolean>;
  onMyTurn: boolean;
}
