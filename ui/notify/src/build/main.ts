import { attributesModule, classModule, init } from 'snabbdom';
import makeCtrl from '../ctrl';
import type { Ctrl, NotifyOpts } from '../interfaces';
import view from '../view';

const patch = init([classModule, attributesModule]);

function main(opts: NotifyOpts): Ctrl {
  const element = document.getElementById('notify-app')!;
  const ctrl = makeCtrl(opts, redraw);

  element.innerHTML = '';
  let vnode = patch(element, view(ctrl));
  function redraw(): void {
    vnode = patch(vnode, view(ctrl));
  }

  if (opts.data) ctrl.update(opts.data, opts.incoming);
  else ctrl.loadPage(1);

  return ctrl;
}

window.lishogi.registerModule(__bundlename__, main);
