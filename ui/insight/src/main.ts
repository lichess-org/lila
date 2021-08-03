import { attributesModule, classModule, init } from 'snabbdom';

import Ctrl from './ctrl';
import { Env } from './interfaces';
import view from './view';

const patch = init([classModule, attributesModule]);

export default function (element: Element, opts: Env) {
  const ctrl = new Ctrl(opts, element, redraw);

  const blueprint = view(ctrl);
  let vnode = patch(element, blueprint);

  // Wait until vnode has been intialized to call askQuestion because
  // askQuestion can call redraw
  ctrl.askQuestion();

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  return ctrl;
}
