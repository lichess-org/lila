import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import { Shogiground } from 'shogiground';
import { Api as CgApi } from 'shogiground/api';
import { Config as CgConfig } from 'shogiground/config';
import * as cg from 'shogiground/types';
import { DrawShape } from 'shogiground/draw';
import changeColorHandle from 'common/coordsColor';
import resizeHandle from 'common/resize';
import AnalyseCtrl from './ctrl';

export function render(ctrl: AnalyseCtrl): VNode {
  return h('div.cg-wrap.cgv' + ctrl.cgVersion.js, {
    hook: {
      insert: vnode => {
        ctrl.shogiground = Shogiground(vnode.elm as HTMLElement, makeConfig(ctrl));
        ctrl.setAutoShapes();
        if (ctrl.node.shapes) ctrl.shogiground.setShapes(ctrl.node.shapes as DrawShape[]);
        ctrl.cgVersion.dom = ctrl.cgVersion.js;
      },
      destroy: _ => ctrl.shogiground.destroy(),
    },
  });
}

export function promote(ground: CgApi, key: Key, role: cg.Role) {
  const piece = ground.state.pieces.get(key);
  if (piece) {
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
    sfen: opts.sfen,
    check: opts.check,
    lastMove: opts.lastMove,
    orientation: ctrl.bottomColor(),
    coordinates: pref.coords !== 0 && !ctrl.embed,
    viewOnly: !!ctrl.embed,
    movable: {
      free: false,
      color: opts.movable!.color,
      dests: opts.movable!.dests,
      showDests: pref.destination,
    },
    events: {
      move: ctrl.userMove,
      dropNewPiece: ctrl.userNewPiece,
      insert(elements) {
        if (!ctrl.embed) resizeHandle(elements, ctrl.data.pref.resizeHandle, ctrl.node.ply);
        if (!ctrl.embed && ctrl.data.pref.coords == 1) changeColorHandle();
      },
      select: () => {
        if (ctrl.dropmodeActive && !ctrl.shogiground?.state.dropmode.active) {
          ctrl.dropmodeActive = false;
          ctrl.redraw();
        }
      },
    },
    premovable: {
      enabled: opts.premovable!.enabled,
      showDests: pref.destination,
      events: {
        set: ctrl.onPremoveSet,
      },
    },
    predroppable: {
      enabled: opts.predroppable!.enabled,
      showDropDests: pref.dropDestination && pref.destination,
    },
    dropmode: {
      showDropDests: pref.dropDestination && pref.destination,
      dropDests: opts.dropmode!.dropDests,
    },
    drawable: {
      enabled: !ctrl.embed,
      eraseOnClick: !ctrl.opts.study || !!ctrl.opts.practice,
    },
    highlight: {
      lastMove: pref.highlight,
      check: pref.highlight,
    },
    animation: {
      duration: pref.animationDuration,
    },
    disableContextMenu: true,
    notation: pref.pieceNotation,
  };
  ctrl.study && ctrl.study.mutateCgConfig(config);

  return config;
}
