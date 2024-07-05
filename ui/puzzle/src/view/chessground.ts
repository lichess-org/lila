import resizeHandle from 'common/resize';
import { Config as CgConfig } from 'chessground/config';
import { h, VNode } from 'snabbdom';
import * as Prefs from 'common/prefs';
import PuzzleCtrl from '../ctrl';

export default function (ctrl: PuzzleCtrl): VNode {
  return h('div.cg-wrap', {
    hook: {
      insert: vnode => ctrl.setChessground(site.makeChessground(vnode.elm as HTMLElement, makeConfig(ctrl))),
      destroy: () => ctrl.ground().destroy(),
    },
  });
}

export function makeConfig(ctrl: PuzzleCtrl): CgConfig {
  const opts = ctrl.makeCgOpts();
  return {
    fen: opts.fen,
    orientation: opts.orientation,
    turnColor: opts.turnColor,
    check: opts.check,
    lastMove: opts.lastMove,
    coordinates: ctrl.pref.coords !== Prefs.Coords.Hidden,
    coordinatesOnSquares: ctrl.pref.coords === Prefs.Coords.All,
    addPieceZIndex: ctrl.pref.is3d,
    addDimensionsCssVarsTo: document.body,
    movable: {
      free: false,
      color: opts.movable!.color,
      dests: opts.movable!.dests,
      showDests: ctrl.pref.destination && !ctrl.blindfold(),
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
        resizeHandle(elements, Prefs.ShowResizeHandle.Always, ctrl.node.ply);
      },
    },
    premovable: {
      enabled: opts.premovable!.enabled,
    },
    drawable: {
      enabled: true,
      defaultSnapToValidMove: site.storage.boolean('arrow.snap').getOrDefault(true),
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
