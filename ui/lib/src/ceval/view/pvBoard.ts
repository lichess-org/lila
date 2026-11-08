// no side effects allowed due to re-export by index.ts

import { Chessground as makeChessground } from '@lichess-org/chessground';
import { uciToMove } from '@lichess-org/chessground/util';
import type { VNode } from 'snabbdom';

import { div } from '@/view/snabbdomElements';

import { type CevalHandler } from '../types';

type PvBoardPopup = { el: HTMLElement; cg: ReturnType<typeof makeChessground> };
const pvBoardPopups = new WeakMap<Element, PvBoardPopup>();

export function renderPvBoard(ctrl: CevalHandler): VNode | undefined {
  const ceval = ctrl.ceval;
  const pvBoard = ceval.pvBoard();
  if (!pvBoard) return undefined;
  const { fen, uci } = pvBoard;
  const cgConfig = {
    fen,
    lastMove: uciToMove(uci),
    orientation: ctrl.getOrientation(),
    coordinates: false,
    viewOnly: true,
    drawable: {
      enabled: false,
      visible: false,
    },
  };
  return div('.pv-board-anchor', {
    hook: {
      insert: (vnode: VNode) => {
        const anchor = vnode.elm as HTMLElement;
        const cgWrap = document.createElement('div');
        cgWrap.className = 'cg-wrap is2d';
        const square = document.createElement('div');
        square.className = 'pv-board-square';
        square.appendChild(cgWrap);
        const el = document.createElement('div');
        el.className = 'pv-board';
        el.appendChild(square);
        document.body.appendChild(el);
        positionPvBoard(anchor, el);
        pvBoardPopups.set(anchor, { el, cg: makeChessground(cgWrap, cgConfig) });
      },
      update: (_: VNode, vnode: VNode) => {
        const anchor = vnode.elm as HTMLElement;
        const popup = pvBoardPopups.get(anchor);
        if (!popup) return;
        popup.cg.set(cgConfig);
        positionPvBoard(anchor, popup.el);
      },
      destroy: (vnode: VNode) => {
        const popup = pvBoardPopups.get(vnode.elm as Element);
        popup?.cg.destroy();
        popup?.el.remove();
        pvBoardPopups.delete(vnode.elm as Element);
      },
    },
  });
}

function positionPvBoard(anchor: HTMLElement, popup: HTMLElement): void {
  const box = anchor.closest('.pv_box');
  if (!box) return;
  const rect = box.getBoundingClientRect();
  const size = Math.min(rect.width * 0.8, 240, Math.min(innerWidth, innerHeight) * 0.4);
  const offset = 34;
  const gap = 4;
  const rtl = getComputedStyle(box).direction === 'rtl';
  const left = rtl ? rect.right - offset - size : rect.left + offset;
  let top = rect.bottom + gap;
  // not enough room below: show above the box instead
  if (top + size > innerHeight && rect.top - gap - size >= 0) top = rect.top - gap - size;
  popup.style.width = `${size}px`;
  popup.style.left = `${Math.max(0, Math.min(left, innerWidth - size))}px`;
  popup.style.top = `${top}px`;
}
