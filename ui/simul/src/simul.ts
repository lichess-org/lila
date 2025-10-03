import { init, type VNode, classModule, attributesModule } from 'snabbdom';
import type { SimulOpts } from './interfaces';
import SimulCtrl from './ctrl';
import { wsConnect } from 'lib/socket';

const patch = init([classModule, attributesModule]);

import view from './view/main';

export function initModule(opts: SimulOpts) {
  const element = document.querySelector('main.simul') as HTMLElement;

  opts.socketSend = wsConnect(`/simul/${opts.data.id}/socket/v4`, opts.socketVersion, {
    receive: (t: string, d: any) => ctrl.socket.receive(t, d),
  }).send;
  opts.element = element;
  opts.$side = $('.simul__side').clone();

  let vnode: VNode;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  const ctrl = new SimulCtrl(opts, redraw);

  const blueprint = view(ctrl);
  element.innerHTML = '';
  vnode = patch(element, blueprint);

  redraw();
}
