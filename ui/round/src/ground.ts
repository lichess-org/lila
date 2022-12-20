import { notationFiles, notationRanks } from 'common/notation';
import { predrop, premove } from 'common/pre-sg';
import resizeHandle from 'common/resize';
import { Config } from 'shogiground/config';
import { checksSquareNames, usiToSquareNames } from 'shogiops/compat';
import { forsythToRole, parseSfen, roleToForsyth } from 'shogiops/sfen';
import { Piece, Role } from 'shogiops/types';
import { makeSquare, parseSquare } from 'shogiops/util';
import { handRoles, pieceCanPromote, pieceForcePromote, promote } from 'shogiops/variant/util';
import { h } from 'snabbdom';
import RoundController from './ctrl';
import { RoundData } from './interfaces';
import { plyStep } from './round';
import * as util from './util';

export function makeConfig(ctrl: RoundController): Config {
  const data = ctrl.data,
    variant = data.game.variant.key,
    hooks = ctrl.makeSgHooks(),
    step = plyStep(data, ctrl.ply),
    playing = ctrl.isPlaying(),
    posRes = playing || (step.check && variant === 'chushogi') ? parseSfen(variant, step.sfen, false) : undefined,
    splitSfen = step.sfen.split(' ');
  return {
    sfen: { board: splitSfen[0], hands: splitSfen[2] },
    orientation: boardOrientation(data, ctrl.flip),
    turnColor: step.ply % 2 === 0 ? 'sente' : 'gote',
    activeColor: playing ? data.player.color : undefined,
    lastDests: step.usi ? usiToSquareNames(step.usi) : undefined,
    checks: variant === 'chushogi' && posRes?.isOk ? checksSquareNames(posRes.value) : step.check,
    coordinates: {
      enabled: data.pref.coords !== 0,
      files: notationFiles(data.pref.notation),
      ranks: notationRanks(data.pref.notation),
    },
    highlight: {
      lastDests: data.pref.highlightLastDests,
      check: data.pref.highlightCheck,
    },
    events: {
      move: hooks.onMove,
      drop: hooks.onDrop,
      unselect: (key: Key) => {
        if (ctrl.lionFirstMove && ctrl.lionFirstMove.to === parseSquare(key)) {
          const from = ctrl.lionFirstMove.from,
            to = ctrl.lionFirstMove.to;
          hooks.onUserMove(makeSquare(from), makeSquare(to), false, { premade: false });
        }
      },
      insert(elements) {
        if (elements) resizeHandle(elements, data.pref.resizeHandle, ctrl.ply);
      },
    },
    hands: {
      roles: handRoles(variant),
    },
    movable: {
      free: false,
      dests: playing && posRes ? util.getMoveDests(posRes) : new Map(),
      showDests: data.pref.destination,
      events: {
        after: hooks.onUserMove,
      },
    },
    droppable: {
      free: false,
      dests: playing && posRes ? util.getDropDests(posRes) : new Map(),
      showDests: data.pref.destination && data.pref.dropDestination,
      events: {
        after: hooks.onUserDrop,
      },
    },
    promotion: {
      promotesTo: (role: Role) => {
        return promote(variant)(role);
      },
      movePromotionDialog: (orig: Key, dest: Key) => {
        const piece = ctrl.shogiground.state.pieces.get(orig) as Piece,
          capture = ctrl.shogiground.state.pieces.get(dest) as Piece | undefined;
        return (
          !!piece &&
          pieceCanPromote(variant)(piece, parseSquare(orig)!, parseSquare(dest)!, capture) &&
          !pieceForcePromote(variant)(piece, parseSquare(dest)!)
        );
      },
      forceMovePromotion: (orig: Key, dest: Key) => {
        const piece = ctrl.shogiground.state.pieces.get(orig) as Piece;
        return !!piece && pieceForcePromote(variant)(piece, parseSquare(dest)!);
      },
    },
    forsyth: {
      fromForsyth: forsythToRole(variant),
      toForsyth: roleToForsyth(variant),
    },
    animation: {
      enabled: true,
      duration: data.pref.animationDuration,
    },
    premovable: {
      enabled: data.pref.enablePremove,
      generate: data.pref.enablePremove ? premove(variant) : undefined,
      showDests: data.pref.destination,
    },
    predroppable: {
      enabled: data.pref.enablePremove,
      generate: data.pref.enablePremove ? predrop(variant) : undefined,
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
      squares: [],
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
