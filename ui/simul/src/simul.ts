import { init, type VNode, classModule, attributesModule } from 'snabbdom';

import { wsConnect } from 'lib/socket';

import SimulCtrl from './ctrl';
import type { SimulOpts } from './interfaces';

const patch = init([classModule, attributesModule]);

import type { SimulTpe } from './socket';
import view from './view/main';

export function initModule(opts: SimulOpts) {
  const element = document.querySelector('main.simul') as HTMLElement;

  opts.socketSend = wsConnect<SimulTpe>(`/simul/${opts.data.id}/socket/v4`, opts.socketVersion, {
    receive: (tpe, data) => ctrl.socket.receive({ tpe, data }),
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
