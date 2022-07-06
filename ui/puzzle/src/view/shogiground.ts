import resizeHandle from 'common/resize';
import { Config as SgConfig } from 'shogiground/config';
import { Controller } from '../interfaces';
import { h, VNode } from 'snabbdom';
import { Role } from 'shogiops/types';
import { pieceCanPromote, pieceInDeadZone, promote } from 'shogiops/variantUtil';
import { parseSquare } from 'shogiops/util';

export function renderBoard(ctrl: Controller): VNode {
  return h('div.sg-wrap', {
    hook: {
      insert: vnode => {
        ctrl.shogiground.set(makeConfig(ctrl));
        ctrl.shogiground.attach({ board: vnode.elm as HTMLElement });
      },
    },
  });
}

export function renderHand(ctrl: Controller, pos: 'top' | 'bottom'): VNode {
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

function makeConfig(ctrl: Controller): SgConfig {
  const opts = ctrl.makeSgOpts();
  return {
    sfen: opts.sfen,
    orientation: opts.orientation,
    turnColor: opts.turnColor,
    activeColor: opts.activeColor,
    check: opts.check,
    lastDests: opts.lastDests,
    coordinates: {
      enabled: ctrl.pref.coords !== 0,
      notation: ctrl.pref.notation,
    },
    movable: {
      free: false,
      dests: opts.movable!.dests,
      showDests: ctrl.pref.destination,
    },
    droppable: {
      free: false,
      dests: opts.droppable!.dests,
      showDests: ctrl.pref.destination && ctrl.pref.dropDestination,
    },
    promotion: {
      promotesTo: (role: Role) => {
        return promote('standard')(role);
      },
      movePromotionDialog: (orig: Key, dest: Key) => {
        const piece = ctrl.shogiground.state.pieces.get(orig);
        return (
          !!piece &&
          pieceCanPromote('standard')(piece, parseSquare(orig)!, parseSquare(dest)!) &&
          !pieceInDeadZone('standard')(piece, parseSquare(dest)!)
        );
      },
      forceMovePromotion: (orig: Key, dest: Key) => {
        const piece = ctrl.shogiground.state.pieces.get(orig);
        return !!piece && pieceInDeadZone('standard')(piece, parseSquare(dest)!);
      },
    },
    draggable: {
      enabled: ctrl.pref.moveEvent > 0,
      showGhost: ctrl.pref.highlightLastDests,
      showTouchSquareOverlay: ctrl.pref.squareOverlay,
    },
    selectable: {
      enabled: ctrl.pref.moveEvent !== 1,
    },
    events: {
      move: ctrl.userMove,
      drop: ctrl.userDrop,
      insert(elements) {
        if (elements) resizeHandle(elements, 2, ctrl.vm.node.ply, _ => true);
      },
    },
    premovable: {
      enabled: opts.premovable!.enabled,
    },
    predroppable: {
      enabled: opts.predroppable!.enabled,
    },
    drawable: {
      enabled: true,
    },
    highlight: {
      lastDests: ctrl.pref.highlightLastDests,
      check: ctrl.pref.highlightCheck,
    },
    animation: {
      enabled: true,
      duration: ctrl.pref.animation.duration,
    },
    disableContextMenu: true,
  };
}
