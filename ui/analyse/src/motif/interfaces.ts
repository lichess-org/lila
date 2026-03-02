import type { NormalMove, Square } from 'chessops';

export interface Pin {
  pinned: Square;
  pinner: Square;
  target: Square;
}

export interface Undefended {
  square: Square;
  materialLoss: number;
  principalAttacker: Square;
}

export interface Checkable {
  king: Square;
  check: NormalMove;
}
