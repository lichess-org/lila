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

export type ForecastList = ForecastStep[][];
