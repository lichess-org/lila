import { h } from 'snabbdom';
import * as domData from './data';
import { uciToMove } from 'chessground/util';
import { Api } from 'chessground/api';

export const init = (node: HTMLElement): void => {
  const [fen, orientation, lm] = node.getAttribute('data-state')!.split(',');
  initWith(node, fen, orientation as Color, lm);
};

export const initWith = (node: HTMLElement, fen: string, orientation: Color, lm?: string): void => {
  makeChessground(node, {
    orientation,
    coordinates: false,
    viewOnly: !node.getAttribute('data-playable'),
    fen,
    lastMove: uciToMove(lm),
    drawable: {
      enabled: false,
      visible: false,
    },
  }).then(cg => domData.set(node, 'chessground', cg));
};

export const initAll = (parent?: HTMLElement) =>
  Array.from((parent || document).getElementsByClassName('mini-board--init')).forEach((el: HTMLElement) => {
    el.classList.remove('mini-board--init');
    init(el);
  });

export const fenColor = (fen: string) => (fen.includes(' w') ? 'white' : 'black');

export const renderClock = (color: Color, time: number) =>
  h(`span.mini-game__clock.mini-game__clock--${color}`, {
    attrs: {
      'data-time': time,
      'data-managed': 1,
    },
  });

export const makeChessground = (el: HTMLElement, config: any) =>
  lichess.loadEsm<Api>('chessground.min', { init: { el, config } });
