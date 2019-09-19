import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Chessground } from 'chessground';
import { Api as CgApi } from 'chessground/api';
import { Config as CgConfig } from 'chessground/config';
import * as cg from 'chessground/types';
import { DrawShape } from 'chessground/draw';
import resizeHandle from 'common/resize';
import AnalyseCtrl from './ctrl';

export function render(ctrl: AnalyseCtrl): VNode {
  return h('div.cg-wrap.cgv' + ctrl.cgVersion.js, {
    hook: {
      insert: vnode => {
        ctrl.chessground = Chessground((vnode.elm as HTMLElement), makeConfig(ctrl));
        ctrl.setAutoShapes();
        if (ctrl.node.shapes) ctrl.chessground.setShapes(ctrl.node.shapes as DrawShape[]);
        ctrl.cgVersion.dom = ctrl.cgVersion.js;
      },
      destroy: _ => ctrl.chessground.destroy()
    }
  });
}

export function promote(ground: CgApi, key: Key, role: cg.Role) {
  const pieces = {};
  const piece = ground.state.pieces[key];
  if (piece && piece.role == 'pawn') {
    pieces[key] = {
      color: piece.color,
      role,
      promoted: true
    };
    ground.setPieces(pieces);
  }
}

export function makeConfig(ctrl: AnalyseCtrl): CgConfig {
  const d = ctrl.data, pref = d.pref, opts = ctrl.makeCgOpts(),
    couldDraw = !window.lichess.hasTouchEvents;
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
      rookCastle: pref.rookCastle
    },
    events: {
      move: ctrl.userMove,
      dropNewPiece: ctrl.userNewPiece,
      insert(elements) {
        if (!ctrl.embed) resizeHandle(elements, ctrl.data.pref.resizeHandle, ctrl.node.ply);
      }
    },
    premovable: {
      enabled: opts.premovable!.enabled,
      showDests: pref.destination,
      events: {
        set: ctrl.onPremoveSet
      }
    },
    drawable: {
      enabled: !ctrl.embed && couldDraw,
      eraseOnClick: (!ctrl.opts.study || !!ctrl.opts.practice) && couldDraw
    },
    highlight: {
      lastMove: pref.highlight,
      check: pref.highlight
    },
    animation: {
      duration: pref.animationDuration
    },
    disableContextMenu: true
  };
  ctrl.study && ctrl.study.mutateCgConfig(config);

  return config;
}
