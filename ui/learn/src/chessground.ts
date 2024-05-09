import { Config as CgConfig } from 'chessground/config';
import * as timeouts from './timeouts';
import * as cg from 'chessground/types';
import { h, VNode } from 'snabbdom';
import { RunCtrl } from './run/runCtrl';
import { ChessCtrl } from './chess';
import { arrow } from './util';

export default function (ctrl: RunCtrl): VNode {
  return h('div.cg-wrap', {
    hook: {
      insert: vnode => {
        const el = vnode.elm as HTMLElement;
        ctrl.setChessground(site.makeChessground(el, makeConfig()));
      },
      destroy: () => ctrl.chessground!.destroy(),
    },
  });
}

const makeConfig = (): CgConfig => ({
  fen: '8/8/8/8/8/8/8/8',
  blockTouchScroll: true,
  coordinates: true,
  movable: { free: false, color: undefined },
  drawable: { enabled: false },
  // draggable: { enabled: false },
  // selectable: { enabled: false },
  // TODO:
  // addPieceZIndex: ctrl.config.is3d,
});

export type CgMove = {
  orig: Key;
  dest: Key;
};

export interface Shape {
  orig: Key;
  dest?: Key;
  color?: string;
}

interface GroundOpts {
  chess: ChessCtrl;
  offerIllegalMove?: boolean;
  autoCastle?: boolean;
  orientation: Color;
  onMove(orig: Key, dest: Key): void;
  items: {
    render(pos: unknown, key: Key): VNode | undefined;
  };
  shapes?: Shape[];
}

export const setChessground = (ctrl: RunCtrl, opts: GroundOpts) => {
  // TODO:
  const ground = ctrl.chessground!;
  const check = opts.chess.instance.in_check();
  ground.set({
    fen: opts.chess.fen(),
    lastMove: undefined,
    selected: undefined,
    orientation: opts.orientation,
    coordinates: true,
    // pieceKey: true,
    turnColor: opts.chess.color(),
    check: check,
    autoCastle: opts.autoCastle,
    movable: {
      free: false,
      color: opts.chess.color(),
      dests: opts.chess.dests({ illegal: opts.offerIllegalMove }),
    },
    events: {
      move: opts.onMove,
    },
    // items: opts.items,
    premovable: {
      enabled: true,
    },
    drawable: {
      enabled: true,
      eraseOnClick: true,
    },
    highlight: {
      lastMove: true,
      // dragOver: true,
    },
    animation: {
      enabled: false, // prevent piece animation during transition
      duration: 200,
    },
    disableContextMenu: true,
  });
  setTimeout(function () {
    ground.set({
      animation: {
        enabled: true,
      },
    });
  }, 200);
  if (opts.shapes) ground.setShapes(opts.shapes.slice(0));
  return ground;
};

export function setFen(
  ctrl: RunCtrl,
  fen: string,
  color: Color,
  dests: cg.Dests,
  lastMove?: [Key, Key, ...unknown[]],
) {
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
  ctrl.chessground?.set(config);
}

export function showCapture(ctrl: RunCtrl, move: CgMove) {
  requestAnimationFrame(() => {
    const $square = $('#learn-app piece[data-key=' + move.orig + ']');
    $square.addClass('wriggle');
    timeouts.setTimeout(function () {
      $square.removeClass('wriggle');
      ctrl.chessground?.setShapes([]);
      ctrl.chessground?.move(move.orig, move.dest);
    }, 600);
  });
}

export function showCheckmate(ctrl: RunCtrl, chess: ChessCtrl) {
  const ground = ctrl.chessground!;
  const turn = chess.instance.turn() === 'w' ? 'b' : 'w';
  const fen = [ground.getFen(), turn, '- - 0 1'].join(' ');
  chess.instance.load(fen);
  const kingKey = chess.kingKey(turn === 'w' ? 'black' : 'white');
  const shapes = chess.instance
    .moves({ verbose: true })
    .filter(m => m.to === kingKey)
    .map(m => arrow(m.from + m.to, 'red'));
  // TODO: check that this works instead of the below original
  ground.set({ check: turn === 'w' ? 'black' : 'white' });
  // ground.set({ check: shapes.length ? kingKey : null });
  ground.setShapes(shapes);
}

export function setCheck(ctrl: RunCtrl, chess: ChessCtrl) {
  const checks = chess.checks();
  ctrl.chessground?.set({
    // TODO: check that this works instead of the below original
    check: true,
    // check: checks ? checks[0].dest : null,
  });
  if (checks)
    ctrl.chessground?.setShapes(
      checks.map(function (move) {
        return arrow(move.orig + move.dest, 'yellow');
      }),
    );
}

export function setColorDests(ctrl: RunCtrl, color: Color, dests: cg.Dests) {
  ctrl.chessground?.set({
    turnColor: color,
    movable: {
      color: color,
      dests: dests,
    },
  });
}
