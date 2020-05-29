import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Draughtsground } from 'draughtsground';
import { Api as CgApi } from 'draughtsground/api';
import { Config as CgConfig } from 'draughtsground/config';
import * as cg from 'draughtsground/types';
import { DrawShape } from 'draughtsground/draw';
import resizeHandle from 'common/resize';
import AnalyseCtrl from './ctrl';

export function render(ctrl: AnalyseCtrl): VNode {
  return h('div.cg-wrap.cgv' + ctrl.cgVersion.js, {
    hook: {
      insert: vnode => {
        ctrl.draughtsground = Draughtsground((vnode.elm as HTMLElement), makeConfig(ctrl));
        ctrl.setAutoShapes();
        if (ctrl.node.shapes) ctrl.draughtsground.setShapes(ctrl.node.shapes as DrawShape[]);
        ctrl.cgVersion.dom = ctrl.cgVersion.js;
        if (ctrl.embed && window.lidraughts) {
          const embeddedResize = (window.lidraughts as any).embeddedResize
          if (embeddedResize) {
            embeddedResize();
          }
        }
      },
      destroy: _ => ctrl.draughtsground.destroy()
    }
  });
}

export function promote(ground: CgApi, key: Key, role: cg.Role) {
  const pieces = {};
  const piece = ground.state.pieces[key];
  //if (piece && piece.role == 'pawn') {
  if (piece && piece.role == 'man') {
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
    couldDraw = !window.lidraughts.hasTouchEvents;
  const config = {
    turnColor: opts.turnColor,
    boardSize: d.game.variant.board.size,
    fen: opts.fen,
    lastMove: opts.lastMove,
    captureLength: opts.captureLength,
    orientation: ctrl.bottomColor(),
    coordinates: ctrl.embed ? 0 : pref.coords,
    coordSystem: ctrl.coordSystem(),
    addPieceZIndex: pref.is3d,
    viewOnly: !!ctrl.embed && !ctrl.gamebookPlay(),
    movable: {
      free: false,
      color: opts.movable!.color,
      dests: opts.movable!.dests,
      showDests: pref.destination,
      variant: d.game.variant.key,
    },
    events: {
      move: ctrl.userMove,
      insert(elements) {
        if (!ctrl.embed) resizeHandle(elements, ctrl.data.pref.resizeHandle, ctrl.node.ply);
      }
    },
    premovable: {
      enabled: opts.premovable!.enabled,
      showDests: pref.destination,
      variant: d.game.variant.key,
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
      check: pref.highlight,
      kingMoves: pref.showKingMoves && (d.game.variant.key === 'frisian' || d.game.variant.key === 'frysk')
    },
    animation: {
      duration: pref.animationDuration
    },
    disableContextMenu: true
  };
  ctrl.study && ctrl.study.mutateCgConfig(config);

  return config;

}
