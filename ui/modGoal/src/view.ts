import Ctrl from './ctrl';
import { h, VNode } from 'snabbdom';

export default function view(ctrl: Ctrl): VNode {
  return h('div.mod-goal', [
    h('div.mod-goal__title', [
      'Mod Goals',
      h('i', {
        attrs: {
          'data-icon': '',
        },
        on: {
          click: ctrl.toggleSetup,
        },
      }),
    ]),
  ]);
}
