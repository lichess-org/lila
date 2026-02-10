import { h, type VNode } from 'snabbdom';
import resizeHandle from 'lib/chessgroundResize';
import type { MouchEvent } from '@lichess-org/chessground/types';
import { eventPosition, opposite } from '@lichess-org/chessground/util';
import type EditorCtrl from './ctrl';
import { storage } from 'lib/storage';
import { Chessground as makeChessground } from '@lichess-org/chessground';
import { pubsub } from 'lib/pubsub';
import { ShowResizeHandle } from 'lib/prefs';

export default function (ctrl: EditorCtrl): VNode {
  return h('div.cg-wrap', {
    hook: {
      insert: vnode => {
        const el = vnode.elm as HTMLElement;
        ctrl.chessground = makeChessground(el, makeConfig(ctrl));
        bindEvents(el, ctrl);
      },
      destroy: () => ctrl.chessground!.destroy(),
    },
  });
}

function bindEvents(el: HTMLElement, ctrl: EditorCtrl): void {
  const handler = onMouseEvent(ctrl);
  ['touchstart', 'touchmove', 'mousedown', 'mousemove', 'contextmenu'].forEach(ev =>
    el.addEventListener(ev, handler),
  );
  pubsub.on('board.change', (is3d: boolean) => {
    ctrl.chessground!.state.addPieceZIndex = is3d;
    ctrl.chessground!.redrawAll();
  });
}

const isLeftButton = (e: MouchEvent): boolean => e.buttons === 1 || e.button === 1;

const isLeftClick = (e: MouchEvent): boolean => isLeftButton(e) && !e.ctrlKey;

const isRightClick = (e: MouchEvent): boolean => e.button === 2 || (!!e.ctrlKey && isLeftButton(e));

let downKey: Key | undefined;
let lastKey: Key | undefined;
let placeDelete: boolean | undefined;

function onMouseEvent(ctrl: EditorCtrl): (e: MouchEvent) => void {
  return function (e: MouchEvent): void {
    const sel = ctrl.selected();
    const isMouseOrTouchStart = e.type === 'mousedown' || e.type === 'touchstart';

    // do not generate corresponding mouse event
    // (https://developer.mozilla.org/en-US/docs/Web/API/Touch_events/Supporting_both_TouchEvent_and_MouseEvent)
    if (sel !== 'pointer' && e.cancelable !== false && (e.type === 'touchstart' || e.type === 'touchmove'))
      e.preventDefault();

    if (isLeftClick(e) || e.type === 'touchstart' || e.type === 'touchmove') {
      if (sel === 'pointer' || ctrl.chessground?.state.draggable.current?.newPiece) return;
      const pos = eventPosition(e);
      if (!pos) return;
      const key = ctrl.chessground!.getKeyAtDomPos(pos);
      if (!key) return;
      if (isMouseOrTouchStart) downKey = key;
      if (sel === 'trash') deleteOrHidePiece(ctrl, key, e);
      else {
        const existingPiece = ctrl.chessground!.state.pieces.get(key);
        const piece = {
          color: sel[0],
          role: sel[1],
        };
        const samePiece =
          existingPiece && piece.color === existingPiece.color && piece.role === existingPiece.role;

        if (isMouseOrTouchStart && samePiece) {
          deleteOrHidePiece(ctrl, key, e);
          placeDelete = true;
          const endEvents = { mousedown: 'mouseup', touchstart: 'touchend' };
          document.addEventListener(endEvents[e.type], () => (placeDelete = false), { once: true });
        } else if (!placeDelete && (isMouseOrTouchStart || key !== lastKey)) {
          ctrl.chessground!.setPieces(new Map([[key, piece]]));
          ctrl.onChange();
          ctrl.chessground!.cancelMove();
        }
      }
      lastKey = key;
    } else if (isRightClick(e)) {
      if (sel !== 'pointer') {
        ctrl.chessground!.state.drawable.current = undefined;
        ctrl.chessground!.state.drawable.shapes = [];

        if (e.type === 'contextmenu' && sel !== 'trash') {
          ctrl.chessground!.cancelMove();
          sel[0] = opposite(sel[0]);
          ctrl.redraw();
        }
      }
    }
  };
}

function deleteOrHidePiece(ctrl: EditorCtrl, key: Key, e: Event): void {
  if (e.type === 'touchstart') {
    if (ctrl.chessground!.state.pieces.has(key)) {
      (ctrl.chessground!.state.draggable.current!.element as HTMLElement).style.display = 'none';
      ctrl.chessground!.cancelMove();
    }
    document.addEventListener('touchend', () => deletePiece(ctrl, key), { once: true });
  } else if (e.type === 'mousedown' || key !== downKey) {
    deletePiece(ctrl, key);
  }
}

function deletePiece(ctrl: EditorCtrl, key: Key): void {
  ctrl.chessground!.setPieces(new Map([[key, undefined]]));
  ctrl.onChange();
}

function makeConfig(ctrl: EditorCtrl): CgConfig {
  return {
    fen: ctrl.getFen(),
    orientation: ctrl.options.orientation || 'white',
    coordinates: ctrl.options.coordinates !== false,
    autoCastle: false,
    addPieceZIndex: ctrl.cfg.is3d,
    movable: {
      free: true,
      color: 'both',
    },
    animation: {
      duration: ctrl.cfg.animation.duration,
    },
    premovable: {
      enabled: false,
    },
    drawable: {
      enabled: true,
      defaultSnapToValidMove: storage.boolean('arrow.snap').getOrDefault(true),
    },
    draggable: {
      showGhost: true,
      deleteOnDropOff: true,
    },
    selectable: {
      enabled: false,
    },
    highlight: {
      lastMove: false,
    },
    events: {
      change: ctrl.onChange.bind(ctrl),
      insert(elements) {
        resizeHandle(elements, ShowResizeHandle.Always, 0);
      },
    },
  };
}
