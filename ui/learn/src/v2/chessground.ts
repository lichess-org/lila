import { h, VNode } from 'snabbdom';
import { Config as CgConfig } from 'chessground/config';
import * as cg from 'chessground/types';
import resizeHandle from 'common/resize';
import { RunCtrl } from './run/runCtrl';

export default function (ctrl: RunCtrl): VNode {
  return h('div.cg-wrap', {
    hook: {
      insert: vnode => {
        const el = vnode.elm as HTMLElement;
        ctrl.chessground = site.makeChessground(el, makeConfig(ctrl));
      },
      destroy: () => ctrl.chessground!.destroy(),
    },
  });
}

const makeConfig = (ctrl: RunCtrl): CgConfig => ({
  // TODO: these are from coordinate trainer
  // fen: ctrl.boardFEN(),
  // orientation: ctrl.orientation,
  blockTouchScroll: true,
  // coordinates: ctrl.showCoordinates(),
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
