// no side effects allowed due to re-export by index.ts

import { Chessground as makeChessground } from '@lichess-org/chessground';
import { uciToMove } from '@lichess-org/chessground/util';
import type { VNode } from 'snabbdom';

import { onInsert } from '@/view';
import { div } from '@/view/snabbdomElements';

import { type CevalHandler } from '../types';

type PvBoardPopup = {
  el: HTMLElement;
  cg: CgApi;
};
let popup: PvBoardPopup | undefined;

export function renderPvBoard(ctrl: CevalHandler): VNode | undefined {
  const pvBoard = ctrl.ceval.pvBoard();
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
      ...onInsert(anchor => createPvBoard(anchor, cgConfig)),
      update: () => popup?.cg.set(cgConfig),
      destroy,
    },
  });
}

function createPvBoard(anchor: HTMLElement, config: CgConfig): void {
  destroy();
  const popupId = 'ceval-pv-board-popup';
  const popupHtml = `<div id="${popupId}" class="pv-board"><div class="pv-board-square"><div class="cg-wrap is2d"/></div></div>`;
  document.body.insertAdjacentHTML('beforeend', popupHtml);
  const cgEl = document.querySelector(`#${popupId} .cg-wrap`) as HTMLElement;
  popup = { el: document.getElementById(popupId)!, cg: makeChessground(cgEl, config) };
  const box = anchor.closest('.pv_box');
  if (!box) return;
  const rect = box.getBoundingClientRect();
  const size = Math.min(rect.width * 0.8, 240, Math.min(innerWidth, innerHeight) * 0.4);
  const left = rect.left + 34;
  const top = rect.bottom + 4;
  popup.el.style.width = `${size}px`;
  popup.el.style.left = `${Math.max(0, Math.min(left, innerWidth - size))}px`;
  popup.el.style.top = `${top}px`;
}

function destroy(): void {
  popup?.cg.destroy();
  popup?.el.remove();
  popup = undefined;
}
