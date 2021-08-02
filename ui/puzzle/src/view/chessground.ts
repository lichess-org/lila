import changeColorHandle from 'common/coordsColor';
import resizeHandle from 'common/resize';
import { Chessground } from 'chessground';
import { Config as CgConfig } from 'chessground/config';
import { Controller } from '../interfaces';
import { h, VNode } from 'snabbdom';

export default function (ctrl: Controller): VNode {
  return h('div.cg-wrap', {
    hook: {
      insert: vnode => ctrl.ground(Chessground(vnode.elm as HTMLElement, makeConfig(ctrl))),
      destroy: _ => ctrl.ground()!.destroy(),
    },
  });
}

export function makeConfig(ctrl: Controller): CgConfig {
  const opts = ctrl.makeCgOpts();
  return {
    fen: opts.fen,
    orientation: opts.orientation,
    turnColor: opts.turnColor,
    check: opts.check,
    lastMove: opts.lastMove,
    coordinates: ctrl.pref.coords !== Prefs.Coords.Hidden,
    addPieceZIndex: ctrl.pref.is3d,
    addDimensionsCssVars: true,
    movable: {
      free: false,
      color: opts.movable!.color,
      dests: opts.movable!.dests,
      showDests: ctrl.pref.destination,
      rookCastle: ctrl.pref.rookCastle,
    },
    draggable: {
      enabled: ctrl.pref.moveEvent > 0,
      showGhost: ctrl.pref.highlight,
    },
    selectable: {
      enabled: ctrl.pref.moveEvent !== 1,
    },
    events: {
      move: ctrl.userMove,
      insert(elements) {
        resizeHandle(elements, Prefs.ShowResizeHandle.Always, ctrl.vm.node.ply);
        if (ctrl.pref.coords === Prefs.Coords.Inside) changeColorHandle();
      },
    },
    premovable: {
      enabled: opts.premovable!.enabled,
    },
    drawable: {
      enabled: true,
      defaultSnapToValidMove: (lichess.storage.get('arrow.snap') || 1) != '0',
    },
    highlight: {
      lastMove: ctrl.pref.highlight,
      check: ctrl.pref.highlight,
    },
    animation: {
      enabled: true,
      duration: ctrl.pref.animation.duration,
    },
    disableContextMenu: true,
  };
}
