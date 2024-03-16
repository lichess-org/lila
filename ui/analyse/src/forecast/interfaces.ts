import { FEN } from 'chessground/types';

export interface ForecastData {
  onMyTurn?: boolean;
  steps?: ForecastStep[][];
}

export interface ForecastStep {
  ply: Ply;
  uci: Uci;
  san: San;
  fen: FEN;
  check?: Key;
}

export type ForecastList = ForecastStep[][];
