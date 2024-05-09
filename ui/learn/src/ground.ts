import type { Square as Key } from 'chess.js';
import { PromotionRole } from './util';

// const cg = new chessground.controller();

export interface Shape {
  orig: Key;
  dest?: Key;
  color?: string;
}

// interface Piece {
//   color: Color;
//   role: PromotionRole;
//   promoted: boolean;
// }

export function promote(key: Key, role: PromotionRole) {
  key;
  role;
  // const pieces: Partial<Record<Key, Piece>> = {};
  // const piece = cg.data.pieces[key];
  // if (piece && piece.role === 'pawn') {
  //   pieces[key] = {
  //     color: piece.color,
  //     role: role,
  //     promoted: true,
  //   };
  //   cg.setPieces(pieces);
  // }
}

// export function data() {
//   return cg.data;
// }

// export function pieces() {
//   return cg.data.pieces;
// }
