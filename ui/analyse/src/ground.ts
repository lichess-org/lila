import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Draughtsground } from 'draughtsground';
import { Api as CgApi } from 'draughtsground/api';
import { Config as CgConfig } from 'draughtsground/config';
import * as cg from 'draughtsground/types';
import { DrawShape } from 'draughtsground/draw';
import AnalyseCtrl from './ctrl';

export function render(ctrl: AnalyseCtrl): VNode {
  return h('div.cg-board-wrap.cgv' + ctrl.cgVersion.js, {
    hook: {
      insert: vnode => {
        ctrl.draughtsground = Draughtsground((vnode.elm as HTMLElement), makeConfig(ctrl));
        ctrl.setAutoShapes();
        if (ctrl.node.shapes) ctrl.draughtsground.setShapes(ctrl.node.shapes as DrawShape[]);
        ctrl.cgVersion.dom = ctrl.cgVersion.js;
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

function makeConfig(ctrl: AnalyseCtrl): CgConfig {

    const d = ctrl.data, pref = d.pref, opts = ctrl.makeCgOpts();

    const config = {
        turnColor: opts.turnColor,
        fen: opts.fen,
        lastMove: opts.lastMove,
        captureLength: opts.captureLength,
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
            dropNewPiece: ctrl.userNewPiece
        },
        premovable: {
            enabled: opts.premovable!.enabled,
            showDests: pref.destination,
            variant: d.game.variant.key
        },
        drawable: {
            enabled: !ctrl.embed,
            eraseOnClick: !ctrl.opts.study || !!ctrl.opts.practice
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
