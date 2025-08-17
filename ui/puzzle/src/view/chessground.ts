import resizeHandle from 'lib/chessgroundResize';
import { h, type VNode } from 'snabbdom';
import { Coords, ShowResizeHandle } from 'lib/prefs';
import type PuzzleCtrl from '../ctrl';
import { storage } from 'lib/storage';
import { Chessground as makeChessground } from '@lichess-org/chessground';

export default function (ctrl: PuzzleCtrl): VNode {
  return h('div.cg-wrap.cgv' + ctrl.cgVersion, {
    hook: {
      insert: vnode => ctrl.setChessground(makeChessground(vnode.elm as HTMLElement, makeConfig(ctrl))),
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
    coordinates: ctrl.pref.coords !== Coords.Hidden,
    coordinatesOnSquares: ctrl.pref.coords === Coords.All,
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
        resizeHandle(elements, ShowResizeHandle.Always, ctrl.node.ply);
      },
    },
    premovable: {
      enabled: opts.premovable!.enabled,
    },
    drawable: {
      enabled: true,
      defaultSnapToValidMove: storage.boolean('arrow.snap').getOrDefault(true),
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
