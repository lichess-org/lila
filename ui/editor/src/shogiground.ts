import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import { Shogiground } from 'shogiground';
import { Config as CgConfig } from 'shogiground/config';
import { MouchEvent } from 'shogiground/types';
import * as util from 'shogiground/util';
import changeColorHandle from 'common/coordsColor';
import EditorCtrl from './ctrl';

export default function (ctrl: EditorCtrl): VNode {
  return h('div.cg-wrap', {
    hook: {
      insert: vnode => {
        const el = vnode.elm as HTMLElement;
        ctrl.shogiground = Shogiground(el, makeConfig(ctrl));
        bindEvents(el, ctrl);
      },
      destroy: _ => ctrl.shogiground!.destroy()
    }
  });
}

function bindEvents(el: HTMLElement, ctrl: EditorCtrl): void {
  const handler = onMouseEvent(ctrl);
  ['touchstart', 'touchmove', 'mousedown', 'mousemove', 'contextmenu'].forEach(function (ev) {
    el.addEventListener(ev, handler)
  });
}

function isLeftButton(e: MouchEvent): boolean {
  return e.buttons === 1 || e.button === 1;
}

function isLeftClick(e: MouchEvent): boolean {
  return isLeftButton(e) && !e.ctrlKey;
}

function isRightClick(e: MouchEvent): boolean {
  return util.isRightButton(e) || (e.ctrlKey && isLeftButton(e));
}

let downKey: Key | undefined;
let lastKey: Key | undefined;
let placeDelete: boolean | undefined;

function onMouseEvent(ctrl: EditorCtrl): (e: MouchEvent) => void {
  return function (e: MouchEvent): void {
    const sel = ctrl.selected();

    // do not generate corresponding mouse event
    // (https://developer.mozilla.org/en-US/docs/Web/API/Touch_events/Supporting_both_TouchEvent_and_MouseEvent)
    if (sel !== 'pointer' && e.cancelable !== false && (e.type === 'touchstart' || e.type === 'touchmove')) e.preventDefault();

    if (isLeftClick(e) || e.type === 'touchstart' || e.type === 'touchmove') {
      if (sel === 'pointer' || (ctrl.shogiground && ctrl.shogiground.state.draggable.current && ctrl.shogiground.state.draggable.current.newPiece)) return;
      const pos = util.eventPosition(e);
      if (!pos) return;
      const key = ctrl.shogiground!.getKeyAtDomPos(pos);
      if (!key) return;
      if (e.type === 'mousedown' || e.type === 'touchstart') downKey = key;
      if (sel === 'trash') deleteOrHidePiece(ctrl, key, e);
      else {
        const existingPiece = ctrl.shogiground!.state.pieces.get(key);
        const piece = {
          color: sel[0],
          role: sel[1]
        };
        const samePiece = existingPiece && piece.color == existingPiece.color && piece.role == existingPiece.role;

        if ((e.type === 'mousedown' || e.type === 'touchstart') && samePiece) {
          deleteOrHidePiece(ctrl, key, e);
          placeDelete = true;
          const endEvents = { mousedown: 'mouseup', touchstart: 'touchend' };
          document.addEventListener(endEvents[e.type], () => placeDelete = false, { once: true });
        } else if (!placeDelete && (e.type === 'mousedown' || e.type === 'touchstart' || key !== lastKey)) {
          ctrl.shogiground!.setPieces(new Map([[key, piece]]));
          ctrl.onChange();
          ctrl.shogiground!.cancelMove();
        }
      }
      lastKey = key;
    } else if (isRightClick(e)) {
      if (sel !== 'pointer') {
        ctrl.shogiground!.state.drawable.current = undefined;
        ctrl.shogiground!.state.drawable.shapes = [];

        if (e.type === 'contextmenu' && sel != 'trash') {
          ctrl.shogiground!.cancelMove();
          sel[0] = util.opposite(sel[0]);
          ctrl.redraw();
        }
      }
    }
  };
}

function deleteOrHidePiece(ctrl: EditorCtrl, key: Key, e: Event): void {
  if (e.type === 'touchstart') {
    if (ctrl.shogiground!.state.pieces.has(key)) {
      (ctrl.shogiground!.state.draggable.current!.element as HTMLElement).style.display = 'none';
      ctrl.shogiground!.cancelMove();
    }
    document.addEventListener('touchend', () => deletePiece(ctrl, key), { once: true });
  } else if (e.type === 'mousedown' || key !== downKey) {
    deletePiece(ctrl, key);
  }
}

function deletePiece(ctrl: EditorCtrl, key: Key): void {
  ctrl.shogiground!.setPieces(new Map([[key, undefined]]));
  ctrl.onChange();
}

function makeConfig(ctrl: EditorCtrl): CgConfig {
  return {
    fen: ctrl.cfg.fen,
    orientation: ctrl.options.orientation || 'white',
    coordinates: !ctrl.cfg.embed,
    autoCastle: false,
    addPieceZIndex: ctrl.cfg.is3d,
    movable: {
      free: true,
      color: 'both'
    },
    animation: {
      duration: ctrl.cfg.animation.duration
    },
    premovable: {
      enabled: false
    },
    drawable: {
      enabled: true
    },
    draggable: {
      showGhost: true,
      deleteOnDropOff: true
    },
    selectable: {
      enabled: false
    },
    highlight: {
      lastMove: false
    },
    events: {
      change: ctrl.onChange.bind(ctrl),
      insert: changeColorHandle
    },
    notation: ctrl.cfg.pieceNotation
  };
}
