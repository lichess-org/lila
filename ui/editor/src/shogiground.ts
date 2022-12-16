import { notationFiles, notationRanks } from 'common/notation';
import { Config as SgConfig } from 'shogiground/config';
import { forsythToRole, roleToForsyth } from 'shogiops/sfen';
import { handRoles } from 'shogiops/variant/util';
import { VNode, h } from 'snabbdom';
import EditorCtrl from './ctrl';

export function renderBoard(ctrl: EditorCtrl): VNode {
  return h('div.sg-wrap', {
    hook: {
      insert: vnode => {
        ctrl.shogiground.attach({ board: vnode.elm as HTMLElement });
      },
    },
  });
}

export function renderHand(ctrl: EditorCtrl, pos: 'top' | 'bottom'): VNode {
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

export function makeConfig(ctrl: EditorCtrl): SgConfig {
  const splitSfen = ctrl.data.sfen.split(' ');
  return {
    sfen: { board: splitSfen[0], hands: splitSfen[2] },
    activeColor: 'both',
    orientation: ctrl.options.orientation || 'sente',
    coordinates: {
      enabled: !ctrl.data.embed,
      files: notationFiles(ctrl.data.pref.notation),
      ranks: notationRanks(ctrl.data.pref.notation),
    },
    hands: {
      roles: handRoles(ctrl.data.variant),
    },
    movable: {
      free: true,
    },
    droppable: {
      free: true,
    },
    animation: {
      duration: ctrl.data.pref.animation,
    },
    premovable: {
      enabled: false,
    },
    drawable: {
      enabled: true,
    },
    draggable: {
      enabled: ctrl.data.pref.moveEvent > 0,
      showGhost: ctrl.data.pref.highlightLastDests,
      showTouchSquareOverlay: ctrl.data.pref.squareOverlay,
      deleteOnDropOff: true,
      addToHandOnDropOff: true,
    },
    selectable: {
      enabled: ctrl.data.pref.moveEvent !== 1,
    },
    forsyth: {
      fromForsyth: forsythToRole(ctrl.rules),
      toForsyth: roleToForsyth(ctrl.rules),
    },
    highlight: {
      lastDests: false,
    },
    events: {
      change: ctrl.onChange.bind(ctrl),
    },
  };
}
