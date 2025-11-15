import { init, type VNode, classModule, attributesModule } from 'snabbdom';
import type { SwissOpts } from './interfaces';
import SwissCtrl from './ctrl';
import { wsConnect } from 'lib/socket';

const patch = init([classModule, attributesModule]);

import view from './view/main';

export function initModule(opts: SwissOpts) {
  const element = document.querySelector('main.swiss') as HTMLElement;

  opts.classes = element.getAttribute('class');
  opts.socketSend = wsConnect('/swiss/' + opts.data.id, opts.data.socketVersion || 0, {
    receive: (t: string, d: any) => ctrl.socket.receive(t, d),
  }).send;
  opts.element = element;
  opts.$side = $('.swiss__side').clone();

  let vnode: VNode;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  const ctrl = new SwissCtrl(opts, redraw);

  const blueprint = view(ctrl);
  element.innerHTML = '';
  vnode = patch(element, blueprint);

  redraw();
}
