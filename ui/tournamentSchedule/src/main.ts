/// <reference types="types/lichess" />

import view from './view';

import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'
import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';

const patch = init([klass, attributes]);

export function app(element: HTMLElement, env: any) {

  let vnode: VNode, ctrl = {
    data: env.data,
    update: d => {
      env.data = d;
      redraw();
    },
    trans: window.lichess.trans(env.i18n)
  };

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  vnode = patch(element, view(ctrl));

  setInterval(redraw, 3700);

  return {
    update: ctrl.update
  };
};
