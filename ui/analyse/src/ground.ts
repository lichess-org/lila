import { h, VNode } from 'snabbdom';
import { Config as SgConfig } from 'shogiground/config';
import * as sg from 'shogiground/types';
import { DrawShape } from 'shogiground/draw';
import resizeHandle from 'common/resize';
import AnalyseCtrl from './ctrl';
import { Role } from 'shogiops/types';
import { handRoles, pieceCanPromote, pieceInDeadZone, promote as shogiPromote } from 'shogiops/variantUtil';
import { parseSquare } from 'shogiops/util';

export function renderBoard(ctrl: AnalyseCtrl): VNode {
  return h('div.sg-wrap', {
    hook: {
      insert: vnode => {
        ctrl.shogiground.attach({
          board: vnode.elm as HTMLElement,
        });
        ctrl.setAutoShapes();
        ctrl.setShapes(ctrl.node.shapes as DrawShape[] | undefined);
      },
    },
  });
}

export function renderHand(ctrl: AnalyseCtrl, pos: 'top' | 'bottom'): VNode {
  return h(`div.sg-hand-wrap.hand-${pos}`, {
    hook: {
      insert: vnode => {
        ctrl.shogiground.attach({
          hands: {
            top: pos === 'top' ? (vnode.elm as HTMLElement) : undefined,
            bottom: pos === 'bottom' ? (vnode.elm as HTMLElement) : undefined,
          },
        });
      },
    },
  });
}

export function makeConfig(ctrl: AnalyseCtrl): SgConfig {
  const d = ctrl.data,
    pref = d.pref,
    opts = ctrl.makeSgOpts();
  const config: SgConfig = {
    turnColor: opts.turnColor,
    activeColor: opts.activeColor,
    sfen: {
      board: opts.sfen?.board,
      hands: opts.sfen?.hands,
    },
    check: opts.check,
    lastDests: opts.lastDests,
    orientation: ctrl.bottomColor(),
    viewOnly: !!ctrl.embed,
    hands: {
      roles: handRoles(ctrl.data.game.variant.key),
    },
    movable: {
      free: false,
      dests: opts.movable!.dests,
      showDests: pref.destination,
    },
    droppable: {
      free: false,
      dests: opts.droppable!.dests,
      showDests: pref.dropDestination && pref.destination,
    },
    coordinates: {
      enabled: pref.coords !== 0 && !ctrl.embed,
      notation: pref.notation,
    },
    events: {
      move: ctrl.userMove,
      drop: ctrl.userDrop,
      insert(boardEls?: sg.BoardElements, _handEls?: sg.HandElements) {
        if (!ctrl.embed && boardEls) resizeHandle(boardEls, ctrl.data.pref.resizeHandle, ctrl.node.ply);
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
      showDests: pref.dropDestination && pref.destination,
    },
    drawable: {
      enabled: !ctrl.embed,
      eraseOnClick: !ctrl.opts.study || !!ctrl.opts.practice,
    },
    promotion: {
      promotesTo: (role: Role) => shogiPromote(ctrl.data.game.variant.key)(role),
      movePromotionDialog: (orig: Key, dest: Key) => {
        const piece = ctrl.shogiground.state.pieces.get(orig);
        return (
          !!piece &&
          pieceCanPromote(ctrl.data.game.variant.key)(piece, parseSquare(orig)!, parseSquare(dest)!) &&
          !pieceInDeadZone(ctrl.data.game.variant.key)(piece, parseSquare(dest)!)
        );
      },
      forceMovePromotion: (orig: Key, dest: Key) => {
        const piece = ctrl.shogiground.state.pieces.get(orig);
        return !!piece && pieceInDeadZone(ctrl.data.game.variant.key)(piece, parseSquare(dest)!);
      },
    },
    highlight: {
      lastDests: pref.highlightLastDests,
      check: pref.highlightCheck,
    },
    draggable: {
      enabled: pref.moveEvent > 0,
      showGhost: pref.highlightLastDests,
      showTouchSquareOverlay: pref.squareOverlay,
    },
    selectable: {
      enabled: pref.moveEvent !== 1,
    },
    animation: {
      duration: pref.animationDuration,
    },
    disableContextMenu: true,
  };
  ctrl.study && ctrl.study.mutateSgConfig(config);

  return config;
}
