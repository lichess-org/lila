import { wsConnect } from 'lib/socket';
import { mountApp } from 'lib/view';

import makeCtrl from './ctrl';
import type { TournamentOpts } from './interfaces';
import view from './view/main';

export function initModule(opts: TournamentOpts) {
  document.body.dataset.tournamentId = opts.data.id;
  opts.socketSend = wsConnect(`/tournament/${opts.data.id}/socket/v5`, opts.data.socketVersion, {
    receive: (t: string, d: any) => ctrl.socket.receive(t, d),
  }).send;
  opts.element = document.querySelector('main.tour') as HTMLElement;
  opts.classes = opts.element.getAttribute('class');
  opts.$side = $('.tour__side').clone();
  opts.$faq = $('.tour__faq').clone();

  opts.element.innerHTML = '';
  const ctrl = mountApp({
    el: opts.element,
    makeCtrl: redraw => new makeCtrl(opts, redraw),
    view,
  });
}
