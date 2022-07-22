export type FEN = string;
export type San = string;
export type Uci = string;

export interface Node {
  fen: FEN;
  check: boolean;
  san?: San;
  uci?: Uci;
}

export interface ReplayData {
  nodes: Node[];
}

export interface ReplayOpts {
  pgn: string;
  orientation?: Color;
  i18n: I18nDict;
}
