import { Config as CgConfig } from 'chessground/config';
import { h, VNode } from 'snabbdom';
import { RunCtrl } from './run/runCtrl';

export interface Shape {
  orig: Key;
  dest?: Key;
  color?: string;
}

export type CgMove = {
  orig: Key;
  dest: Key;
};

export default function (ctrl: RunCtrl): VNode {
  return h('div.cg-wrap', {
    hook: {
      insert: vnode => {
        const el = vnode.elm as HTMLElement;
        el.addEventListener('contextmenu', e => e.preventDefault());
        ctrl.setChessground(site.makeChessground(el, makeConfig(ctrl)));
      },
      destroy: () => ctrl.chessground!.destroy(),
    },
  });
}

const makeConfig = (ctrl: RunCtrl): CgConfig => ({
  fen: '8/8/8/8/8/8/8/8',
  blockTouchScroll: true,
  coordinates: true,
  coordinatesOnSquares: ctrl.pref.coords === Prefs.Coords.All,
  movable: { free: false, color: undefined },
  drawable: { enabled: false },
  draggable: { enabled: true },
  selectable: { enabled: true },
});
