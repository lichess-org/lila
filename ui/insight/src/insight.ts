import { attributesModule, classModule, init } from 'snabbdom';

import Ctrl from './ctrl';
import type { Env } from './interfaces';
import { registerMultipleSelect } from './multipleSelect';
import { view } from './view';

const patch = init([classModule, attributesModule]);

registerMultipleSelect();

export function initModule(opts: Env) {
  const element = document.getElementById('insight')!;
  const ctrl = new Ctrl(opts, element, redraw);

  const blueprint = view(ctrl);
  let vnode = patch(element, blueprint);

  // Wait until vnode has been initialized to call askQuestion because
  // askQuestion can call redraw
  ctrl.askQuestion();

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  return ctrl;
}
