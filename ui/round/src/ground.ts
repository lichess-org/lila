import { h } from 'snabbdom';
import { Config } from 'shogiground/config';
import resizeHandle from 'common/resize';
import * as util from './util';
import { plyStep } from './round';
import RoundController from './ctrl';
import { RoundData } from './interfaces';
import { handRoles, pieceCanPromote, pieceInDeadZone, promote } from 'shogiops/variantUtil';
import { parseSquare, Role } from 'shogiops';
import { usiToSquareNames } from 'shogiops/compat';

export function makeConfig(ctrl: RoundController): Config {
  const data = ctrl.data,
    hooks = ctrl.makeSgHooks(),
    step = plyStep(data, ctrl.ply),
    playing = ctrl.isPlaying(),
    splitSfen = step.sfen.split(' ');
  return {
    sfen: { board: splitSfen[0], hands: splitSfen[2] },
    orientation: boardOrientation(data, ctrl.flip),
    turnColor: step.ply % 2 === 0 ? 'sente' : 'gote',
    activeColor: playing ? data.player.color : undefined,
    lastDests: step.usi ? (usiToSquareNames(step.usi) as Key[]) : undefined,
    check: !!step.check,
    coordinates: {
      enabled: data.pref.coords !== 0,
      notation: data.pref.notation,
    },
    highlight: {
      lastDests: data.pref.highlightLastDests,
      check: data.pref.highlightCheck,
    },
    events: {
      move: hooks.onMove,
      drop: hooks.onDrop,
      insert(elements) {
        if (elements) resizeHandle(elements, ctrl.data.pref.resizeHandle, ctrl.ply);
      },
    },
    hands: {
      roles: handRoles(ctrl.data.game.variant.key),
    },
    movable: {
      free: false,
      dests: playing ? util.getMoveDests(step.sfen, data.game.variant.key) : new Map(),
      showDests: data.pref.destination,
      events: {
        after: hooks.onUserMove,
      },
    },
    droppable: {
      free: false,
      dests: playing ? util.getDropDests(step.sfen, data.game.variant.key) : new Map(),
      showDests: data.pref.destination && data.pref.dropDestination,
      events: {
        after: hooks.onUserDrop,
      },
    },
    promotion: {
      promotesTo: (role: Role) => {
        return promote(ctrl.data.game.variant.key)(role);
      },
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
    animation: {
      enabled: true,
      duration: data.pref.animationDuration,
    },
    premovable: {
      enabled: data.pref.enablePremove,
      showDests: data.pref.destination,
    },
    predroppable: {
      enabled: data.pref.enablePremove,
      showDests: data.pref.destination && data.pref.dropDestination,
    },
    draggable: {
      enabled: data.pref.moveEvent > 0,
      showGhost: data.pref.highlightLastDests,
      showTouchSquareOverlay: data.pref.squareOverlay,
    },
    selectable: {
      enabled: data.pref.moveEvent !== 1,
    },
    drawable: {
      enabled: true,
    },
  };
}

export function reload(ctrl: RoundController) {
  ctrl.shogiground.set(makeConfig(ctrl));
}

export function boardOrientation(data: RoundData, flip: boolean): Color {
  return flip ? data.opponent.color : data.player.color;
}

export function renderBoard(ctrl: RoundController) {
  return h('div.sg-wrap', {
    hook: {
      insert: vnode => {
        ctrl.shogiground.attach({ board: vnode.elm as HTMLElement });
        // ctrl.setKeyboardMove();
      },
    },
  });
}

export function renderHand(ctrl: RoundController, pos: 'top' | 'bottom') {
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
