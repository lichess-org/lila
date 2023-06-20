import { h, VNode } from 'snabbdom';
import { Chessground } from 'chessground';
import { Config as CgConfig } from 'chessground/config';
import * as cg from 'chessground/types';
import resizeHandle from 'common/resize';
import CoordinateTrainerCtrl from './ctrl';

export default function (ctrl: CoordinateTrainerCtrl): VNode {
  return h('div.cg-wrap', {
    hook: {
      insert: vnode => {
        const el = vnode.elm as HTMLElement;
        ctrl.chessground = Chessground(el, makeConfig(ctrl));
      },
      destroy: () => ctrl.chessground!.destroy(),
    },
  });
}

function makeConfig(ctrl: CoordinateTrainerCtrl): CgConfig {
  return {
    fen: ctrl.boardFEN(),
    orientation: ctrl.orientation,
    blockTouchScroll: true,
    coordinates: ctrl.showCoordinates(),
    addPieceZIndex: ctrl.config.is3d,
    movable: { free: false, color: undefined },
    drawable: { enabled: false },
    draggable: { enabled: false },
    selectable: { enabled: false },
    events: {
      insert(elements: cg.Elements) {
        resizeHandle(elements, ctrl.config.resizePref, ctrl.playing ? 2 : 0);
      },
      select: ctrl.onChessgroundSelect,
    },
  };
}
