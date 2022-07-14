import ReplayCtrl from './ctrl';
import { Chessground } from 'chessground';
import { Config as CgConfig } from 'chessground/config';
import { h, VNode } from 'snabbdom';

export default function view(ctrl: ReplayCtrl) {
  return h('div.replay', [h('div.replay__board', renderGround(ctrl)), renderControls(ctrl)]);
}

const renderControls = (ctrl: ReplayCtrl) =>
  h('div.replay__controls', [
    dirButton('', ctrl.index < 1, ctrl.backward),
    dirButton('', ctrl.index > ctrl.nodes.length - 2, ctrl.forward),
  ]);

const dirButton = (icon: string, disabled: boolean, click: () => void) =>
  h('button.fbt', {
    attrs: {
      'data-icon': icon,
    },
    class: { disabled },
    on: { click },
  });

const renderGround = (ctrl: ReplayCtrl): VNode =>
  h('div.cg-wrap', {
    hook: {
      insert: vnode => ctrl.ground(Chessground(vnode.elm as HTMLElement, makeConfig(ctrl))),
    },
  });

export const makeConfig = (ctrl: ReplayCtrl): CgConfig => ({
  viewOnly: true,
  coordinates: true,
  addDimensionsCssVars: true,
  drawable: {
    enabled: false,
    visible: false,
  },
  ...ctrl.cgOpts(),
});
