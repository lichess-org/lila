import resizeHandle from 'common/resize';
import { Chessground } from 'chessground';
import { Config as CgConfig } from 'chessground/config';
import PuzzleController from '../ctrl';
import { h, VNode } from 'snabbdom';
import * as Prefs from 'common/prefs';

export default function (ctrl: PuzzleController): VNode {
  return h('div.cg-wrap', {
    hook: {
      insert: vnode => ctrl.setChessground(Chessground(vnode.elm as HTMLElement, makeConfig(ctrl))),
      destroy: _ => ctrl.ground()!.destroy(),
    },
  });
}

export function makeConfig(ctrl: PuzzleController): CgConfig {
  const opts = ctrl.makeCgOpts(),
    pref = ctrl.opts.pref;
  return {
    fen: opts.fen,
    orientation: opts.orientation,
    turnColor: opts.turnColor,
    check: opts.check,
    lastMove: opts.lastMove,
    coordinates: pref.coords !== Prefs.Coords.Hidden,
    addPieceZIndex: pref.is3d,
    addDimensionsCssVarsTo: document.body,
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
        resizeHandle(elements, Prefs.ShowResizeHandle.Always, ctrl.vm.node.ply);
      },
    },
    premovable: {
      enabled: opts.premovable!.enabled,
    },
    drawable: {
      enabled: true,
      defaultSnapToValidMove: lichess.storage.boolean('arrow.snap').getOrDefault(true),
    },
    highlight: {
      lastMove: pref.highlight,
      check: pref.highlight,
    },
    animation: {
      enabled: true,
      duration: pref.animation.duration,
    },
    disableContextMenu: true,
  };
}
