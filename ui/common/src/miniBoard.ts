import { h, VNode } from 'snabbdom';
import * as domData from './data';
import { uciToMove } from 'chessground/util';

export const initMiniBoard = (node: HTMLElement): void => {
  const [fen, orientation, lm] = node.getAttribute('data-state')!.split(',');
  initMiniBoardWith(node, fen, orientation as Color, lm);
};

export const initMiniBoardWith = (node: HTMLElement, fen: string, orientation: Color, lm?: string): void => {
  domData.set(
    node,
    'chessground',
    site.makeChessground(node, {
      orientation,
      coordinates: false,
      viewOnly: !node.getAttribute('data-playable'),
      fen,
      lastMove: uciToMove(lm),
      drawable: {
        enabled: false,
        visible: false,
      },
    }),
  );
};

export const initMiniBoards = (parent?: HTMLElement): void =>
  Array.from((parent || document).getElementsByClassName('mini-board--init')).forEach((el: HTMLElement) => {
    el.classList.remove('mini-board--init');
    initMiniBoard(el);
  });

export const fenColor = (fen: string): Color => (fen.includes(' w') ? 'white' : 'black');

export const renderClock = (color: Color, time: number): VNode =>
  h(`span.mini-game__clock.mini-game__clock--${color}`, {
    attrs: { 'data-time': time, 'data-managed': 1 },
  });
