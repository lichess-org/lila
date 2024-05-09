import * as timeouts from './timeouts';
import { ChessCtrl } from './chess';
import type { Square as Key } from 'chess.js';
import { PromotionRole } from './util';

// const cg = new chessground.controller();

export type CgMove = {
  orig: Key;
  dest: Key;
};

export interface Shape {
  orig: Key;
  dest?: Key;
  color?: string;
}

export type Dests = Partial<Record<Key, Key[]>>;

export const stop = () => {
  // TODO:
  // cg.stop();
};

export function color(color: Color, dests: Dests) {
  color;
  dests;
  // cg.set({
  //   turnColor: color,
  //   movable: {
  //     color: color,
  //     dests: dests,
  //   },
  // });
}

export function fen(fen: string, color: Color, dests: Dests, lastMove?: [Key, Key, ...unknown[]]) {
  const config = {
    turnColor: color,
    fen: fen,
    movable: {
      color: color,
      dests: dests,
    },
    // Casting here instead of declaring lastMove as [Key, Key] right away
    // allows the fen function to accept [orig, dest, promotion] values
    // for lastMove as well.
    lastMove: lastMove as [Key, Key],
  };
  config;
  // cg.set(config);
}

export function check(chess: ChessCtrl) {
  chess;
  // const checks = chess.checks();
  // cg.set({
  //   check: checks ? checks[0].dest : null,
  // });
  // if (checks)
  //   cg.setShapes(
  //     checks.map(function (move) {
  //       return arrow(move.orig + move.dest, 'yellow');
  //     }),
  //   );
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

export function showCapture(move: CgMove) {
  requestAnimationFrame(function () {
    const $square = $('#learn-app piece[data-key=' + move.orig + ']');
    $square.addClass('wriggle');
    timeouts.setTimeout(function () {
      $square.removeClass('wriggle');
      // cg.setShapes([]);
      // cg.apiMove(move.orig, move.dest);
    }, 600);
  });
}

export function showCheckmate(chess: ChessCtrl) {
  chess;
  // const turn = chess.instance.turn() === 'w' ? 'b' : 'w';
  // const fen = [cg.getFen(), turn, '- - 0 1'].join(' ');
  // chess.instance.load(fen);
  // const kingKey = chess.kingKey(turn === 'w' ? 'black' : 'white');
  // const shapes = chess.instance
  //   .moves({ verbose: true })
  //   .filter(m => m.to === kingKey)
  //   .map(m => arrow(m.from + m.to, 'red'));
  // cg.set({ check: shapes.length ? kingKey : null });
  // cg.setShapes(shapes);
}

// export function setShapes(shapes: Shape[]) {
//   cg.setShapes(shapes);
// }

export function resetShapes() {
  // cg.setShapes([]);
}

// export const select = cg.selectSquare;
