import * as domData from './data';

export const init = (node: HTMLElement): void => {
  const [sfen, orientation, lm] = node.getAttribute('data-state')!.split(',');
  initWith(node, sfen, orientation as Color, lm);
};

export const initWith = (node: HTMLElement, sfen: string, orientation: Color, lm?: string): void => {
  if (!window.Shogiground) setTimeout(() => init(node), 500);
  else {
    domData.set(
      node,
      'shogiground',
      window.Shogiground(node, {
        orientation,
        coordinates: false,
        viewOnly: !node.getAttribute('data-playable'),
        resizable: false,
        sfen: sfen,
        hasPockets: true,
        pockets: sfen && sfen.split(' ').length > 2 ? sfen.split(' ')[2] : '',
        lastMove: lm ? (lm[1] === '*' ? [lm.slice(2)] : [lm[0] + lm[1], lm[2] + lm[3]]) : undefined,
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
