import { wsConnect } from 'lib/socket';
import { mountApp } from 'lib/view';

import SimulCtrl from './ctrl';
import type { SimulOpts } from './interfaces';
import type { SimulTpe } from './socket';
import view from './view/main';

export function initModule(opts: SimulOpts) {
  const element = document.querySelector('main.simul') as HTMLElement;

  opts.socketSend = wsConnect<SimulTpe>(`/simul/${opts.data.id}/socket/v4`, opts.socketVersion, {
    receive: (tpe, data) => ctrl.socket.receive({ tpe, data }),
  }).send;
  opts.element = element;
  opts.$side = $('.simul__side').clone();

  element.innerHTML = '';
  const ctrl = mountApp({
    el: element,
    makeCtrl: redraw => new SimulCtrl(opts, redraw),
    view,
  });

  ctrl.redraw();
}
