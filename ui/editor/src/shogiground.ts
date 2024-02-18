import resizeHandle from 'common/resize';
import { notationFiles, notationRanks } from 'common/notation';
import { Config as SgConfig } from 'shogiground/config';
import { forsythToRole, roleToForsyth } from 'shogiops/sfen';
import { handRoles, promote, unpromote } from 'shogiops/variant/util';
import { VNode, h } from 'snabbdom';
import EditorCtrl from './ctrl';
import { BoardElements, HandElements } from 'shogiground/types';

export function renderBoard(ctrl: EditorCtrl): VNode {
  return h('div.sg-wrap', {
    hook: {
      insert: vnode => {
        ctrl.shogiground.attach({ board: vnode.elm as HTMLElement });
      },
      destroy: () => {
        ctrl.shogiground.detach({ board: true });
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
      files: notationFiles(),
      ranks: notationRanks(),
    },
    hands: {
      roles: handRoles(ctrl.data.variant),
      inlined: ctrl.data.variant !== 'chushogi',
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
    promotion: {
      promotesTo: promote(ctrl.rules),
      unpromotesTo: unpromote(ctrl.rules),
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
      forceSpares: true,
      addSparesToHand: true,
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
      insert(boardEls?: BoardElements, _handEls?: HandElements) {
        if (!ctrl.data.embed && boardEls) resizeHandle(boardEls, ctrl.data.pref.resizeHandle, { visible: () => true });
      },
    },
  };
}
