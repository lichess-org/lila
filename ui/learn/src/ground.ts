import * as chessground from 'chessground';
import * as timeouts from './timeouts';
import { ChessCtrl } from './chess';
import type { Square as Key } from 'chess.js';
import { arrow, PromotionRole } from './util';
import { MNode } from './mithrilFix';

const cg = new (chessground as any).controller();

export type CgMove = {
  orig: Key;
  dest: Key;
};

export const instance = cg;

export interface Shape {
  orig: Key;
  dest?: Key;
  color?: string;
}

export type Dests = Partial<Record<Key, Key[]>>;

interface GroundOpts {
  chess: ChessCtrl;
  offerIllegalMove?: boolean;
  autoCastle?: boolean;
  orientation: 'black' | 'white';
  onMove(orig: Key, dest: Key): void;
  items: {
    render(pos: unknown, key: Key): MNode | undefined;
  };
  shapes?: Shape[];
}

export function set(opts: GroundOpts) {
  const check = opts.chess.instance.in_check();
  cg.set({
    fen: opts.chess.fen(),
    lastMove: null,
    selected: null,
    orientation: opts.orientation,
    coordinates: true,
    pieceKey: true,
    turnColor: opts.chess.color(),
    check: check,
    autoCastle: opts.autoCastle,
    movable: {
      free: false,
      color: opts.chess.color(),
      dests: opts.chess.dests({
        illegal: opts.offerIllegalMove,
      }),
    },
    events: {
      move: opts.onMove,
    },
    items: opts.items,
    premovable: {
      enabled: true,
    },
    drawable: {
      enabled: true,
      eraseOnClick: true,
    },
    highlight: {
      lastMove: true,
      dragOver: true,
    },
    animation: {
      enabled: false, // prevent piece animation during transition
      duration: 200,
    },
    disableContextMenu: true,
  });
  setTimeout(function () {
    cg.set({
      animation: {
        enabled: true,
      },
    });
  }, 200);
  if (opts.shapes) cg.setShapes(opts.shapes.slice(0));
  return cg;
}

export const stop = cg.stop;

export function color(color: string, dests: Dests) {
  cg.set({
    turnColor: color,
    movable: {
      color: color,
      dests: dests,
    },
  });
}

export function fen(fen: string, color: string, dests: Dests, lastMove?: [Key, Key, ...unknown[]]) {
  const config = {
    turnColor: color,
    fen: fen,
    movable: {
      color: color,
      dests: dests,
    },
    lastMove: lastMove,
  };
  cg.set(config);
}

export function check(chess: ChessCtrl) {
  const checks = chess.checks();
  cg.set({
    check: checks ? checks[0].dest : null,
  });
  if (checks)
    cg.setShapes(
      checks.map(function (move) {
        return arrow(move.orig + move.dest, 'yellow');
      })
    );
}

interface Piece {
  color: string;
  role: PromotionRole;
  promoted: boolean;
}

export function promote(key: Key, role: PromotionRole) {
  const pieces: Partial<Record<Key, Piece>> = {};
  const piece = cg.data.pieces[key];
  if (piece && piece.role === 'pawn') {
    pieces[key] = {
      color: piece.color,
      role: role,
      promoted: true,
    };
    cg.setPieces(pieces);
  }
}

export function data() {
  return cg.data;
}

export function pieces() {
  return cg.data.pieces;
}

export function get(key: Key) {
  return cg.data.pieces[key];
}

export function showCapture(move: CgMove) {
  requestAnimationFrame(function () {
    const $square = $('#learn-app piece[data-key=' + move.orig + ']');
    $square.addClass('wriggle');
    timeouts.setTimeout(function () {
      $square.removeClass('wriggle');
      cg.setShapes([]);
      cg.apiMove(move.orig, move.dest);
    }, 600);
  });
}

export function showCheckmate(chess: ChessCtrl) {
  const turn = chess.instance.turn() === 'w' ? 'b' : 'w';
  const fen = [cg.getFen(), turn, '- - 0 1'].join(' ');
  chess.instance.load(fen);
  const kingKey = chess.kingKey(turn === 'w' ? 'black' : 'white');
  const shapes = chess.instance
    .moves({
      verbose: true,
    })
    .filter(function (m) {
      return m.to === kingKey;
    })
    .map(function (m) {
      return arrow(m.from + m.to, 'red');
    });
  cg.set({
    check: shapes.length ? kingKey : null,
  });
  cg.setShapes(shapes);
}

export function setShapes(shapes: Shape[]) {
  cg.setShapes(shapes);
}

export function resetShapes() {
  cg.setShapes([]);
}

export const select = cg.selectSquare;
