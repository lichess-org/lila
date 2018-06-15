import view from './view';

import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'
import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';
import dragscroll from 'dragscroll';

const patch = init([klass, attributes]);

dragscroll // required to include the dependency :( :( :(

export function app(element: HTMLElement, env: any) {

  let vnode: VNode, ctrl = {
    data: () => env.data,
    trans: window.lichess.trans(env.i18n)
  };

  function redraw() {
    vnode = patch(vnode || element, view(ctrl));
  }

  redraw();

  setInterval(redraw, 3700);

  return {
    update: d => {
      env.data = d;
      redraw();
    }
  };
};
