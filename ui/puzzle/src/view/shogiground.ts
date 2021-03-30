import changeColorHandle from 'common/coordsColor';
import resizeHandle from 'common/resize';
import { Shogiground } from 'shogiground';
import { Config as CgConfig } from 'shogiground/config';
import { Controller } from '../interfaces';
import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

export default function (ctrl: Controller): VNode {
  return h('div.cg-wrap', {
    hook: {
      insert: vnode => ctrl.ground(Shogiground(vnode.elm as HTMLElement, makeConfig(ctrl))),
      destroy: _ => ctrl.ground()!.destroy(),
    },
  });
}

function makeConfig(ctrl: Controller): CgConfig {
  const opts = ctrl.makeCgOpts();
  return {
    fen: opts.fen,
    orientation: opts.orientation,
    turnColor: opts.turnColor,
    check: opts.check,
    lastMove: opts.lastMove,
    coordinates: ctrl.pref.coords !== 0,
    addPieceZIndex: ctrl.pref.is3d,
    movable: {
      free: false,
      color: opts.movable!.color,
      dests: opts.movable!.dests,
      showDests: ctrl.pref.destination,
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
      dropNewPiece: ctrl.userDrop,
      insert(elements) {
        resizeHandle(elements, 2, ctrl.vm.node.ply, _ => true);
        if (ctrl.pref.coords == 1) changeColorHandle();
      },
    },
    premovable: {
      enabled: opts.premovable!.enabled,
    },
    drawable: {
      enabled: true,
    },
    highlight: {
      lastMove: ctrl.pref.highlight,
      check: ctrl.pref.highlight,
    },
    animation: {
      enabled: true,
      duration: ctrl.pref.animation.duration,
    },
    notation: ctrl.pref.pieceNotation ?? 0,
    disableContextMenu: true,
  };
}
