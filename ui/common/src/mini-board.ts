import * as domData from './data';

export const init = (node: HTMLElement): void => {
  const [fen, orientation, lm] = node.getAttribute('data-state')!.split(',');
  initWith(node, fen, orientation as Color, lm);
}

export const initWith = (node: HTMLElement, fen: string, orientation: Color, lm?: string): void => {
  if (!window.Chessground) setTimeout(() => init(node), 500);
  else {
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
}

export const initAll = (parent?: HTMLElement) =>
  Array.from((parent || document).getElementsByClassName('mini-board--init')).forEach(init);
