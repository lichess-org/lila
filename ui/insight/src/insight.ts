import { attributesModule, classModule, init } from 'snabbdom';

import Ctrl from './ctrl';
import { Env } from './interfaces';
import { view } from './view';
import { registerMultipleSelect } from './multipleSelect';

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

export function registerFormHandler() {
  $('form.insight-refresh').on('submit', function (this: HTMLFormElement) {
    fetch(this.action, {
      method: 'post',
      credentials: 'same-origin',
    }).then(site.reload);

    $(this).replaceWith($(this).find('.crunching').removeClass('none'));

    return false;
  });
}
