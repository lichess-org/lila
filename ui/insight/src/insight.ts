import { mountApp } from 'lib/view';

import Ctrl from './ctrl';
import type { Env } from './interfaces';
import { registerMultipleSelect } from './multipleSelect';
import { view } from './view';

registerMultipleSelect();

export function initModule(opts: Env) {
  const element = document.getElementById('insight')!;
  const ctrl = mountApp({
    el: element,
    makeCtrl: redraw => new Ctrl(opts, element, redraw),
    view,
  });

  // Wait until vnode has been initialized to call askQuestion because
  // askQuestion can call redraw
  ctrl.askQuestion();

  return ctrl;
}
