import { h, VNode } from 'snabbdom';
import { Chessground } from 'chessground';
import { Api as CgApi } from 'chessground/api';
import { Config as CgConfig } from 'chessground/config';
import * as cg from 'chessground/types';
import { DrawShape } from 'chessground/draw';
import changeColorHandle from 'common/coordsColor';
import resizeHandle from 'common/resize';
import AnalyseCtrl from './ctrl';

export function render(ctrl: AnalyseCtrl): VNode {
  return h('div.cg-wrap.cgv' + ctrl.cgVersion.js, {
    hook: {
      insert: vnode => {
        ctrl.chessground = Chessground(vnode.elm as HTMLElement, makeConfig(ctrl));
        ctrl.setAutoShapes();
        if (ctrl.node.shapes) ctrl.chessground.setShapes(ctrl.node.shapes as DrawShape[]);
        ctrl.cgVersion.dom = ctrl.cgVersion.js;
      },
      destroy: _ => ctrl.chessground.destroy(),
    },
  });
}

export function promote(ground: CgApi, key: Key, role: cg.Role) {
  const piece = ground.state.pieces.get(key);
  if (piece && piece.role == 'pawn') {
    ground.setPieces(
      new Map([
        [
          key,
          {
            color: piece.color,
            role,
            promoted: true,
          },
        ],
      ])
    );
  }
}

export function makeConfig(ctrl: AnalyseCtrl): CgConfig {
  const d = ctrl.data,
    pref = d.pref,
    opts = ctrl.makeCgOpts();
  const config = {
    turnColor: opts.turnColor,
    fen: opts.fen,
    check: opts.check,
    lastMove: opts.lastMove,
    orientation: ctrl.bottomColor(),
    coordinates: pref.coords !== 0 && !ctrl.embed,
    addPieceZIndex: pref.is3d,
    viewOnly: !!ctrl.embed,
    movable: {
      free: false,
      color: opts.movable!.color,
      dests: opts.movable!.dests,
      showDests: pref.destination,
      rookCastle: pref.rookCastle,
    },
    events: {
      move: ctrl.userMove,
      dropNewPiece: ctrl.userNewPiece,
      insert(elements: cg.Elements) {
        if (!ctrl.embed) resizeHandle(elements, 2, ctrl.node.ply);
        if (!ctrl.embed && ctrl.data.pref.coords == 1) changeColorHandle();
      },
    },
    premovable: {
      enabled: opts.premovable!.enabled,
      showDests: pref.destination,
      events: {
        set: ctrl.onPremoveSet,
      },
    },
    drawable: {
      enabled: !ctrl.embed,
      eraseOnClick: !ctrl.opts.study || !!ctrl.opts.practice,
      defaultSnapToValidMove: (lichess.storage.get('arrow.snap') || 1) != '0',
    },
    highlight: {
      lastMove: pref.highlight,
      check: pref.highlight,
    },
    animation: {
      duration: pref.animationDuration,
    },
    disableContextMenu: true,
  };
  ctrl.study && ctrl.study.mutateCgConfig(config);

  return config;
}
