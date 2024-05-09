import { Config as CgConfig } from 'chessground/config';
import { h, VNode } from 'snabbdom';
import * as cg from 'chessground/types';
import resizeHandle from 'common/resize';
import { RunCtrl } from './run/runCtrl';
import { ChessCtrl } from './chess';

export default function (ctrl: RunCtrl): VNode {
  return h('div.cg-wrap', {
    hook: {
      insert: vnode => {
        const el = vnode.elm as HTMLElement;
        ctrl.setChessground(site.makeChessground(el, makeConfig(ctrl)));
      },
      destroy: () => ctrl.chessground!.destroy(),
    },
  });
}

const makeConfig = (ctrl: RunCtrl): CgConfig => ({
  fen: '8/8/8/8/8/8/8/8',
  blockTouchScroll: true,
  coordinates: false,
  // TODO: below are from coordinate trainer
  // addPieceZIndex: ctrl.config.is3d,
  movable: { free: false, color: undefined },
  drawable: { enabled: false },
  draggable: { enabled: false },
  selectable: { enabled: false },
  events: {
    insert(elements: cg.Elements) {
      resizeHandle(elements, 0, 0);
      ctrl;
      // resizeHandle(elements, ctrl.config.resizePref, ctrl.playing ? 2 : 0);
    },
    // select: ctrl.onChessgroundSelect,
  },
});

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
      // dests: opts.chess.dests({
      //   illegal: opts.offerIllegalMove,
      // }),
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

export function getPiece(ctrl: RunCtrl, key: Key) {
  return ctrl.chessground!.state.pieces.get(key);
}
