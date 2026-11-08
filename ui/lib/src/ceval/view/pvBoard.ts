// no side effects allowed due to re-export by index.ts

import { Chessground as makeChessground } from '@lichess-org/chessground';
import { uciToMove } from '@lichess-org/chessground/util';
import type { VNode } from 'snabbdom';

import * as domData from '@/data';
import { onInsert } from '@/view';
import { div } from '@/view/snabbdomElements';

import { type CevalHandler } from '../types';

const popupId = 'ceval-pv-board-popup';
const popupHtml = `<div id="${popupId}" class="pv-board"><div class="pv-board-square"><div class="cg-wrap is2d"></div></div></div>`;

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
      ...onInsert(anchor => {
        document.body.insertAdjacentHTML('beforeend', popupHtml);
        const popup = getPopup()!;
        domData.set(popup, 'chessground', makeChessground(popup, cgConfig));
        positionPvBoard(anchor);
      }),
      update: () => {
        console.log('update');
        const popup = getPopup()!;
        if (!popup) return;
        domData.get(popup, 'chessground')?.set(cgConfig);
        // positionPvBoard(anchor);
      },
      destroy: () => {
        // const popup = getPopup()!;
        // if (popup) {
        //   domData.get(popup, 'chessground')?.destroy();
        //   popup.remove();
        // }
      },
    },
  });
}

const getPopup = () => document.getElementById(popupId);

function positionPvBoard(anchor: HTMLElement): void {
  const popup = getPopup();
  if (!popup) return;
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
