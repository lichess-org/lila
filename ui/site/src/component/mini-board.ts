import * as domData from 'common/data';

export const init = (node: HTMLElement) => {
  if (!window.Chessground) return setTimeout(() => init(node), 500);
  const $el = $(node).removeClass('mini-board--init'),
    [fen, orientation, lm] = $el.data('state').split(',');
  domData.set(node, 'chessground', window.Chessground(node, {
    orientation,
    coordinates: false,
    viewOnly: !node.getAttribute('data-playable'),
    resizable: false,
    fen,
    lastMove: lm && (lm[1] === '@' ? [lm.slice(2)] : [lm[0] + lm[1], lm[2] + lm[3]]),
    drawable: {
      enabled: false,
      visible: false
    }
  }));
}

export const initAll = (parent?: HTMLElement) =>
  Array.from((parent || document).getElementsByClassName('mini-board--init')).forEach(init);
