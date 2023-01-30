import { Config } from 'shogiground/config';
import { usiToSquareNames } from 'shogiops/compat';
import * as domData from './data';

export const init = (node: HTMLElement): void => {
  const [sfen, orientation, lm] = node.getAttribute('data-state')!.split(',');
  initWith(node, sfen, orientation as Color, lm);
};

export const initWith = (node: HTMLElement, sfen: string, orientation: Color, lm?: string): void => {
  if (!window.Shogiground) setTimeout(() => init(node), 500);
  else {
    const splitSfen = sfen.split(' '),
      config: Config = {
        orientation,
        coordinates: {
          enabled: false,
        },
        viewOnly: !node.getAttribute('data-playable'),
        sfen: { board: splitSfen[0], hands: splitSfen[2] },
        hands: {
          inlined: true,
        },
        lastDests: lm ? usiToSquareNames(lm) : undefined,
        drawable: {
          enabled: false,
          visible: false,
        },
      };
    domData.set(node, 'shogiground', window.Shogiground(config, { board: node }));
  }
};

export const initAll = (parent?: HTMLElement) =>
  Array.from((parent || document).getElementsByClassName('mini-board--init')).forEach(init);
