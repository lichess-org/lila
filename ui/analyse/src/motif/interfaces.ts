import type { NormalMove } from 'chessops';

export interface Pin {
  pinned: number;
  pinner: number;
  target: number;
}

export interface Undefended {
  square: number;
  materialLoss: number;
  principalAttacker: number;
}

export interface Checkable {
  king: number;
  check: NormalMove;
}
