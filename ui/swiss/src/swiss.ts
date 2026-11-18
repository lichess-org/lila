import { wsConnect } from 'lib/socket';
import { mountApp } from 'lib/view';

import SwissCtrl from './ctrl';
import type { SwissOpts } from './interfaces';
import view from './view/main';

export function initModule(opts: SwissOpts) {
  const element = document.querySelector('main.swiss') as HTMLElement;

  opts.classes = element.getAttribute('class');
  opts.socketSend = wsConnect('/swiss/' + opts.data.id, opts.data.socketVersion || 0, {
    receive: (t: string, d: any) => ctrl.socket.receive(t, d),
  }).send;
  opts.element = element;
  opts.$side = $('.swiss__side').clone();

  element.innerHTML = '';
  const ctrl = mountApp({
    el: element,
    makeCtrl: redraw => new SwissCtrl(opts, redraw),
    view,
  });

  ctrl.redraw();
}
