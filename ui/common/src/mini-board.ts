import * as domData from './data';
import { uciToMove } from 'chessground/util';

export const init = (node: HTMLElement): void => {
  const [fen, orientation, lm] = node.getAttribute('data-state')!.split(',');
  initWith(node, fen, orientation as Color, lm);
};

export const initWith = (node: HTMLElement, fen: string, orientation: Color, lm?: string): void => {
  if (!window.Chessground) setTimeout(() => init(node), 500);
  else {
    domData.set(
      node,
      'chessground',
      window.Chessground(node, {
        orientation,
        coordinates: false,
        viewOnly: !node.getAttribute('data-playable'),
        fen,
        lastMove: uciToMove(lm),
        drawable: {
          enabled: false,
          visible: false,
        },
      })
    );
  }
};

export const initAll = (parent?: HTMLElement) =>
  Array.from((parent || document).getElementsByClassName('mini-board--init')).forEach(init);
