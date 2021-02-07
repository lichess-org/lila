import StormCtrl from '../ctrl';
import changeColorHandle from 'common/coordsColor';
import resizeHandle from 'common/resize';
import { Chessground } from 'chessground';
import { Config as CgConfig } from 'chessground/config';
import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

export default function (ctrl: StormCtrl): VNode {
  return h('div.cg-wrap', {
    hook: {
      insert: vnode => ctrl.ground(Chessground(vnode.elm as HTMLElement, makeConfig(ctrl))),
      destroy: _ => ctrl.withGround(g => g.destroy()),
    },
  });
}

function makeConfig(ctrl: StormCtrl): CgConfig {
  const opts = ctrl.makeCgOpts();
  const pref = ctrl.pref;
  return {
    fen: opts.fen,
    orientation: opts.orientation,
    turnColor: opts.turnColor,
    check: opts.check,
    lastMove: opts.lastMove,
    coordinates: pref.coords !== 0,
    addPieceZIndex: pref.is3d,
    movable: {
      free: false,
      color: opts.movable!.color,
      dests: opts.movable!.dests,
      showDests: pref.destination,
      rookCastle: pref.rookCastle,
    },
    draggable: {
      enabled: pref.moveEvent > 0,
      showGhost: pref.highlight,
    },
    selectable: {
      enabled: pref.moveEvent !== 1,
    },
    events: {
      move: ctrl.userMove,
      insert(elements) {
        resizeHandle(elements, 1, 0, p => p == 0);
        if (pref.coords == 1) changeColorHandle();
      },
    },
    premovable: {
      enabled: false,
    },
    drawable: {
      enabled: true,
      defaultSnapToValidMove: (lichess.storage.get('arrow.snap') || 1) != '0',
    },
    highlight: {
      lastMove: pref.highlight,
      check: pref.highlight,
    },
    animation: {
      enabled: false,
    },
    disableContextMenu: true,
  };
}
